package com.eventledger.accountservice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AccountServiceApplicationTest {

    @Test
    void testMainMethod() {
        assertDoesNotThrow(() -> AccountServiceApplication.main(new String[]{"--server.port=0", "--spring.main.web-application-type=none"}));
    }
}

