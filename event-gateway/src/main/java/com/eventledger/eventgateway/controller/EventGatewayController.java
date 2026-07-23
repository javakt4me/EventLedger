package com.eventledger.eventgateway.controller;

import com.eventledger.eventgateway.client.AccountServiceClient;
import com.eventledger.eventgateway.dto.ErrorResponse;
import com.eventledger.eventgateway.dto.EventRequest;
import com.eventledger.eventgateway.dto.EventResponse;
import com.eventledger.eventgateway.metrics.EventGatewayMetrics;
import com.eventledger.eventgateway.service.EventGatewayService;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventGatewayController {

    private static final Logger logger = LoggerFactory.getLogger(EventGatewayController.class);
    private final EventGatewayService eventGatewayService;
    private final EventGatewayMetrics metrics;

    public EventGatewayController(EventGatewayService eventGatewayService, EventGatewayMetrics metrics) {
        this.eventGatewayService = eventGatewayService;
        this.metrics = metrics;
    }

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody EventRequest request) {
        Timer.Sample sample = null;
        try {
            // metrics may be a test double that returns null; guard for null to avoid NPEs in tests
            sample = (metrics != null) ? metrics.startEventCreationTimer() : null;
            if (metrics != null) {
                metrics.incrementActiveEvents();
            }
        
            logger.debug("Creating event for account: {}", request.getAccountId());
            EventResponse response = eventGatewayService.createEvent(request);
            metrics.recordEventCreated();
            // Respect the explicit 'created' flag on EventResponse for idempotency semantics.
            // Tests expect that only responses with created==true return 201; otherwise return 200.
            boolean created = response.isCreated();
            logger.info("Event processed for account: {} (created={})", request.getAccountId(), created);
            if (created) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (IllegalArgumentException ex) {
            metrics.recordEventError();
            logger.error("Validation error creating event: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("VALIDATION_ERROR", ex.getMessage(), 400));
        } catch (AccountServiceClient.ServiceUnavailableException ex) {
            metrics.recordDownstreamError();
            logger.error("Account service unavailable: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("SERVICE_UNAVAILABLE", ex.getMessage(), 503));
        } catch (Exception ex) {
            metrics.recordEventError();
            logger.error("Internal error creating event: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", ex.getMessage(), 500));
        } finally {
            try {
                if (sample != null && metrics != null && metrics.eventCreationTimer() != null) {
                    sample.stop(metrics.eventCreationTimer());
                }
            } catch (Exception e) {
                logger.warn("Failed to stop creation timer: {}", e.getMessage());
            }
            try {
                if (metrics != null) {
                    metrics.decrementActiveEvents();
                }
            } catch (Exception e) {
                logger.warn("Failed to decrement active events: {}", e.getMessage());
            }
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(@PathVariable String id) {
        Timer.Sample sample = null;

        try {
            sample = (metrics != null) ? metrics.startEventRetrievalTimer() : null;
            logger.debug("Retrieving event: {}", id);
            EventResponse response = eventGatewayService.getEvent(id);
            metrics.recordEventRetrieval();
            logger.info("Event retrieved successfully: {}", id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            metrics.recordEventError();
            logger.error("Event not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", ex.getMessage(), 404));
        } catch (Exception ex) {
            metrics.recordEventError();
            logger.error("Error retrieving event: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("INTERNAL_ERROR", ex.getMessage(), 500));
        } finally {
            try {
                if (sample != null && metrics != null && metrics.eventRetrievalTimer() != null) {
                    sample.stop(metrics.eventRetrievalTimer());
                }
            } catch (Exception e) {
                logger.warn("Failed to stop retrieval timer: {}", e.getMessage());
            }
        }
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsByAccount(
            @RequestParam(required = false) String account) {
        logger.debug("Retrieving events for account: {}", account);
        try {
            List<EventResponse> responses;
            if (account != null && !account.isEmpty()) {
                responses = eventGatewayService.getEventsByAccount(account);
            } else {
                responses = eventGatewayService.getEventsByAccount("");
            }
            metrics.recordEventRetrieval();
            logger.info("Events retrieved for account: {} (count: {})", account, responses.size());
            return ResponseEntity.ok(responses);
        } catch (Exception ex) {
            metrics.recordEventError();
            logger.error("Error retrieving events for account: {}", account, ex);
            throw ex;
        }
    }
}

