package com.eventledger.eventgateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EventGatewayMetrics {

    private static final Logger logger = LoggerFactory.getLogger(EventGatewayMetrics.class);

    private final Counter eventCreatedCounter;
    private final Counter eventRetrievalCounter;
    private final Counter eventErrorCounter;
    private final Counter downstreamServiceErrors;
    private final Timer eventCreationTimerObj;
    private final Timer eventRetrievalTimerObj;
    private final AtomicInteger activeEvents;

    public EventGatewayMetrics(MeterRegistry meterRegistry) {
        this.eventCreatedCounter = Counter.builder("events.created.total")
                .description("Total number of events created")
                .register(meterRegistry);

        this.eventRetrievalCounter = Counter.builder("events.retrieval.total")
                .description("Total number of event retrievals")
                .register(meterRegistry);

        this.eventErrorCounter = Counter.builder("events.errors.total")
                .description("Total number of event processing errors")
                .register(meterRegistry);

        this.downstreamServiceErrors = Counter.builder("downstream.service.errors.total")
                .description("Total number of downstream service errors")
                .tag("service", "account-service")
                .register(meterRegistry);

        this.eventCreationTimerObj = Timer.builder("events.creation.duration")
                .description("Time taken to create an event")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.eventRetrievalTimerObj = Timer.builder("events.retrieval.duration")
                .description("Time taken to retrieve an event")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        this.activeEvents = meterRegistry.gauge("events.active.count",
                new AtomicInteger(0),
                AtomicInteger::get);

        logger.info("Event Gateway Metrics initialized");
    }

    public void recordEventCreated() {
        eventCreatedCounter.increment();
        logger.debug("Recorded event created metric");
    }

    public void recordEventRetrieval() {
        eventRetrievalCounter.increment();
        logger.debug("Recorded event retrieval metric");
    }

    public void recordEventError() {
        eventErrorCounter.increment();
        logger.debug("Recorded event error metric");
    }

    public void recordDownstreamError() {
        downstreamServiceErrors.increment();
        logger.debug("Recorded downstream service error metric");
    }

    public Timer.Sample startEventCreationTimer() {
        return Timer.start();
    }

    public Timer.Sample startEventRetrievalTimer() {
        return Timer.start();
    }

    public Timer eventCreationTimer() {
        return eventCreationTimerObj;
    }

    public Timer eventRetrievalTimer() {
        return eventRetrievalTimerObj;
    }

    public void incrementActiveEvents() {
        activeEvents.incrementAndGet();
    }

    public void decrementActiveEvents() {
        activeEvents.decrementAndGet();
    }
}

