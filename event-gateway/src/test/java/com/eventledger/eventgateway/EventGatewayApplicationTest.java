package com.eventledger.eventgateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EventGatewayApplicationTest {

    @Test
    void testMainMethod() {
        assertDoesNotThrow(() -> EventGatewayApplication.main(new String[]{"--server.port=0", "--spring.main.web-application-type=none"}));
    }
}

