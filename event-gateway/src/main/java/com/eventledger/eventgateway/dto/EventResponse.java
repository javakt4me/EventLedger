package com.eventledger.eventgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class EventResponse {
    private String eventId;
    private String accountId;
    private String type;
    private BigDecimal amount;
    private String currency;
    private Instant eventTimestamp;
    private Instant receivedAt;
    private String status;
    private String metadata;
    private boolean created = false;

    public EventResponse() {
    }

    public EventResponse(String eventId, String accountId, String type,
                        BigDecimal amount, String currency, Instant eventTimestamp,
                        Instant receivedAt, String status, String metadata) {
        this.eventId = eventId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.eventTimestamp = eventTimestamp;
        this.receivedAt = receivedAt;
        this.status = status;
        this.metadata = metadata;
        // If the constructor is given a receivedAt timestamp or a non-empty status,
        // treat the response as created unless the caller explicitly overrides via setCreated().
        this.created = (this.receivedAt != null) || (this.status != null && !this.status.isEmpty());
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }
}

