package com.eventledger.eventgateway.config;

import com.eventledger.eventgateway.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError());
        assertEquals("Invalid input", response.getBody().getMessage());
        assertEquals(400, response.getBody().getStatus());
    }

    @Test
    void testHandleIllegalArgumentExceptionWithNullMessage() {
        IllegalArgumentException ex = new IllegalArgumentException((String) null);
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError());
        assertEquals(400, response.getBody().getStatus());
    }

    @Test
    void testHandleGlobalException() {
        Exception ex = new RuntimeException("Unexpected error");
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("An unexpected error occurred"));
        assertTrue(response.getBody().getMessage().contains("Unexpected error"));
        assertEquals(500, response.getBody().getStatus());
    }

    @Test
    void testHandleGlobalExceptionWithNullMessage() {
        Exception ex = new RuntimeException((String) null);
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getError());
        assertEquals(500, response.getBody().getStatus());
    }

    @Test
    void testHandleCheckedException() {
        Exception ex = new Exception("Some checked exception");
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().getError());
    }
}
