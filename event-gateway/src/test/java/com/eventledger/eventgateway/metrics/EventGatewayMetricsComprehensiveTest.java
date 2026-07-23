package com.eventledger.eventgateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventGatewayMetricsComprehensiveTest {

    private SimpleMeterRegistry meterRegistry;
    private EventGatewayMetrics metrics;

    @BeforeEach
    void setUp() {
        // Use a real SimpleMeterRegistry to avoid mocking issues
        meterRegistry = new SimpleMeterRegistry();
        metrics = new EventGatewayMetrics(meterRegistry);
    }

    @Test
    void testMultipleMetricsCallsSequence() {
        assertDoesNotThrow(() -> {
            metrics.recordEventCreated();
            metrics.recordEventRetrieval();
            metrics.recordEventError();
            metrics.recordDownstreamError();
            metrics.incrementActiveEvents();
            metrics.decrementActiveEvents();
        });
    }

    @Test
    void testTimerSampleGeneration() {
        Timer.Sample sample1 = metrics.startEventCreationTimer();
        Timer.Sample sample2 = metrics.startEventRetrievalTimer();
        
        assertNotNull(sample1);
        assertNotNull(sample2);
    }

    @Test
    void testMetricsWithRealMeterRegistry() {
        // Verify all methods work correctly with a real registry
        assertDoesNotThrow(() -> {
            metrics.recordEventCreated();
            metrics.recordEventRetrieval();
            metrics.recordEventError();
            metrics.recordDownstreamError();
            metrics.incrementActiveEvents();
            metrics.decrementActiveEvents();
            
            Timer.Sample sample = metrics.startEventCreationTimer();
            assertNotNull(sample);
            
            Timer timer = metrics.eventCreationTimer();
            assertNotNull(timer);
            
            sample = metrics.startEventRetrievalTimer();
            assertNotNull(sample);
            
            timer = metrics.eventRetrievalTimer();
            assertNotNull(timer);
        });
    }
}
