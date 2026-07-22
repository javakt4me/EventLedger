package com.eventledger.eventgateway.util;

import com.eventledger.eventgateway.dto.EventRequest;

import java.math.BigDecimal;

public class ValidationUtil {

    public static String validateEvent(EventRequest request) {
        if (request.getEventId() == null || request.getEventId().trim().isEmpty()) {
            return "Event ID is required";
        }

        if (request.getAccountId() == null || request.getAccountId().trim().isEmpty()) {
            return "Account ID is required";
        }

        if (request.getType() == null || request.getType().trim().isEmpty()) {
            return "Type is required";
        }

        if (!request.getType().equals("CREDIT") && !request.getType().equals("DEBIT")) {
            return "Type must be CREDIT or DEBIT";
        }

        if (request.getAmount() == null) {
            return "Amount is required";
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "Amount must be greater than 0";
        }

        if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
            return "Currency is required";
        }

        if (request.getEventTimestamp() == null) {
            return "Event timestamp is required";
        }

        return null; // No error
    }
}

