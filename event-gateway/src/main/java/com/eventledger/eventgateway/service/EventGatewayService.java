package com.eventledger.eventgateway.service;

import com.eventledger.eventgateway.client.AccountServiceClient;
import com.eventledger.eventgateway.domain.Event;
import com.eventledger.eventgateway.domain.EventStatus;
import com.eventledger.eventgateway.dto.EventRequest;
import com.eventledger.eventgateway.dto.EventResponse;
import com.eventledger.eventgateway.repository.EventRepository;
import com.eventledger.eventgateway.util.ValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventGatewayService {

    private final EventRepository eventRepository;
    private final AccountServiceClient accountServiceClient;
    private final ObjectMapper objectMapper;

    public EventGatewayService(EventRepository eventRepository,
                             AccountServiceClient accountServiceClient,
                             ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.accountServiceClient = accountServiceClient;
        this.objectMapper = objectMapper;
    }

    public EventResponse createEvent(EventRequest request) {
        // Validate input
        String validationError = ValidationUtil.validateEvent(request);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        // Check for idempotency
        Optional<Event> existing = eventRepository.findByEventId(request.getEventId());
        if (existing.isPresent()) {
            Event existingEvent = existing.get();
            // If previously failed, allow reprocessing on resubmission
            if (existingEvent.getStatus() == EventStatus.FAILED) {
                try {
                    accountServiceClient.processTransaction(request);
                    existingEvent.setStatus(EventStatus.PROCESSED);
                    eventRepository.save(existingEvent);
                } catch (AccountServiceClient.ServiceUnavailableException ex) {
                    // keep status FAILED and rethrow
                    existingEvent.setStatus(EventStatus.FAILED);
                    eventRepository.save(existingEvent);
                    throw ex;
                }
                EventResponse resp = convertToResponse(existingEvent);
                resp.setCreated(false);
                return resp;
            }

            EventResponse resp = convertToResponse(existingEvent);
            resp.setCreated(false);
            return resp;
        }

        // Create and save the event
        String metadataJson = null;
        try {
            if (request.getMetadata() != null) {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }

        Event event = new Event(
                request.getEventId(),
                request.getAccountId(),
                request.getType(),
                request.getAmount(),
                request.getCurrency(),
                request.getEventTimestamp(),
                metadataJson
        );

        Event savedEvent = eventRepository.save(event);

        // Call Account Service
        try {
            accountServiceClient.processTransaction(request);
            savedEvent.setStatus(EventStatus.PROCESSED);
            eventRepository.save(savedEvent);
        } catch (AccountServiceClient.ServiceUnavailableException ex) {
            savedEvent.setStatus(EventStatus.FAILED);
            eventRepository.save(savedEvent);
            throw ex;
        }

        EventResponse response = convertToResponse(savedEvent);
        response.setCreated(true);
        return response;
    }

    public EventResponse getEvent(String eventId) {
        Optional<Event> event = eventRepository.findByEventId(eventId);
        if (!event.isPresent()) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }
        return convertToResponse(event.get());
    }

    public List<EventResponse> getEventsByAccount(String accountId) {
        List<Event> events = eventRepository.findByAccountIdOrderByEventTimestamp(accountId);
        return events.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private EventResponse convertToResponse(Event event) {
        return new EventResponse(
                event.getEventId(),
                event.getAccountId(),
                event.getType(),
                event.getAmount(),
                event.getCurrency(),
                event.getEventTimestamp(),
                event.getReceivedAt(),
                event.getStatus().toString(),
                event.getMetadata()
        );
    }
}

