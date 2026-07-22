# ✅ EventLedger API - Complete Working Guide

**Date**: July 22, 2026  
**Status**: All Services Running & Operational  

---

## 🟢 Services Status

- ✅ **Account Service**: http://localhost:8081 - **RUNNING**
- ✅ **Event Gateway**: http://localhost:8080 - **RUNNING**
- ✅ **Distributed Tracing**: Operational (Spring Cloud Sleuth)
- ✅ **Structured JSON Logging**: Active on all services
- ✅ **Service-to-Service Communication**: Fixed and working

---

## 📋 API Endpoints - Complete Reference

### **1. Health Checks**

```bash
# Account Service Health
curl http://localhost:8081/health

# Event Gateway Health
curl http://localhost:8080/health
```

**Response**:
```json
{
  "status": "UP",
  "service": "account-service",
  "timestamp": 1784697744263,
  "database": {
    "status": "UP",
    "connection": "OK"
  }
}
```

---

### **2. Create Event ⭐ CORRECT FORMAT**

```bash
# IMPORTANT: Use these field names and include ALL required fields:
# - eventId: Unique event identifier (REQUIRED)
# - accountId: Account to credit/debit (REQUIRED)
# - type: CREDIT or DEBIT (REQUIRED - NOT "DEPOSIT" or "WITHDRAWAL")
# - amount: Decimal amount (REQUIRED - must be > 0)
# - currency: Currency code, e.g., "USD" (REQUIRED)
# - eventTimestamp: ISO 8601 timestamp (REQUIRED)

curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-123456",
    "accountId": "ACC001",
    "type": "CREDIT",
    "amount": 1000,
    "currency": "USD",
    "eventTimestamp": "2026-07-22T11:00:00.000Z"
  }'
```

**Response**:
```json
{
  "eventId": "evt-123456",
  "accountId": "ACC001",
  "type": "CREDIT",
  "amount": 1000,
  "currency": "USD",
  "eventTimestamp": "2026-07-22T11:00:00.000Z",
  "receivedAt": "2026-07-22T05:30:21.437Z",
  "status": "PROCESSED",
  "metadata": null
}
```

---

### **3. Get Events**

```bash
# Get all events
curl http://localhost:8080/events

# Get events for specific account
curl "http://localhost:8080/events?account=ACC001"
```

**Response**:
```json
[
  {
    "eventId": "evt-123456",
    "accountId": "ACC001",
    "type": "CREDIT",
    "amount": 1000.00,
    "currency": "USD",
    "eventTimestamp": "2026-07-22T11:00:00.000Z",
    "receivedAt": "2026-07-22T05:30:21.437Z",
    "status": "PROCESSED",
    "metadata": null
  }
]
```

---

### **4. Get Account Balance**

```bash
curl http://localhost:8081/accounts/ACC001/balance
```

**Response**:
```json
{
  "accountId": "ACC001",
  "balance": 1000.00,
  "currency": "USD"
}
```

---

### **5. Get Account Details**

```bash
curl http://localhost:8081/accounts/ACC001
```

**Response**:
```json
{
  "accountId": "ACC001",
  "balance": 1000.00,
  "currency": "USD",
  "recentTransactions": [
    {
      "eventId": "evt-123456",
      "accountId": "ACC001",
      "type": "CREDIT",
      "amount": 1000.00,
      "currency": "USD",
      "eventTimestamp": "2026-07-22T11:00:00.000Z",
      "createdAt": "2026-07-22T05:30:21.848Z",
      "metadata": null
    }
  ]
}
```

---

### **6. Metrics Endpoints**

```bash
# View all available metrics
curl http://localhost:8081/actuator/metrics
curl http://localhost:8080/actuator/metrics

# View specific metrics
curl http://localhost:8080/actuator/metrics/events.created.total
curl http://localhost:8081/actuator/metrics/account.transactions.created
```

---

## ❌ Common Mistakes & Fixes

### ❌ Mistake 1: Wrong Field Names
```bash
# WRONG - will cause 400 validation error
curl -X POST http://localhost:8080/events \
  -d '{"eventType":"DEPOSIT","amount":1000}'

# CORRECT - must use "type" not "eventType"
curl -X POST http://localhost:8080/events \
  -d '{"eventId":"evt-1","type":"CREDIT","amount":1000}'
```

### ❌ Mistake 2: Invalid Transaction Type
```bash
# WRONG - "DEPOSIT" is not valid
{"type":"DEPOSIT"}

# CORRECT - must be "CREDIT" or "DEBIT"
{"type":"CREDIT"}
or
{"type":"DEBIT"}
```

### ❌ Mistake 3: Missing Required Fields
```bash
# WRONG - missing eventId and eventTimestamp
curl -X POST http://localhost:8080/events \
  -d '{"accountId":"ACC001","type":"CREDIT","amount":1000}'

# CORRECT - include all required fields
curl -X POST http://localhost:8080/events \
  -d '{
    "eventId":"evt-123",
    "accountId":"ACC001",
    "type":"CREDIT",
    "amount":1000,
    "currency":"USD",
    "eventTimestamp":"2026-07-22T11:00:00.000Z"
  }'
```

### ❌ Mistake 4: Using Docker Service Name Locally
```bash
# Error: SERVICE_UNAVAILABLE with "account-service: UnknownHostException"
# This means Event Gateway is trying to reach Account Service
# using the Docker hostname, but services are running locally.
# This has been FIXED - Account Service URL is now localhost:8081
```

---

## 📊 Test Scenarios

### **Scenario 1: Single Credit Transaction**
```bash
# 1. Create credit event
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId":"evt-001",
    "accountId":"ACC001",
    "type":"CREDIT",
    "amount":500,
    "currency":"USD",
    "eventTimestamp":"2026-07-22T10:00:00.000Z"
  }'

# 2. Check balance - should be 500
curl http://localhost:8081/accounts/ACC001/balance

# Expected: {"accountId":"ACC001","balance":500.00,"currency":"USD"}
```

### **Scenario 2: Multiple Transactions**
```bash
# 1. First credit: +1000
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId":"evt-001",
    "accountId":"ACC002",
    "type":"CREDIT",
    "amount":1000,
    "currency":"USD",
    "eventTimestamp":"2026-07-22T10:00:00.000Z"
  }'

# 2. Second debit: -200
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId":"evt-002",
    "accountId":"ACC002",
    "type":"DEBIT",
    "amount":200,
    "currency":"USD",
    "eventTimestamp":"2026-07-22T10:01:00.000Z"
  }'

# 3. Check balance - should be 800
curl http://localhost:8081/accounts/ACC002/balance

# Expected: {"accountId":"ACC002","balance":800.00,"currency":"USD"}
```

### **Scenario 3: Idempotency Testing**
```bash
# 1. Create event
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId":"evt-idem-001",
    "accountId":"ACC003",
    "type":"CREDIT",
    "amount":1000,
    "currency":"USD",
    "eventTimestamp":"2026-07-22T10:00:00.000Z"
  }'

# 2. Create SAME event again (same eventId)
# Should return the same response without creating duplicate
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId":"evt-idem-001",
    "accountId":"ACC003",
    "type":"CREDIT",
    "amount":1000,
    "currency":"USD",
    "eventTimestamp":"2026-07-22T10:00:00.000Z"
  }'

# 3. Check balance - should be 1000 (not 2000)
curl http://localhost:8081/accounts/ACC003/balance

# Expected: {"accountId":"ACC003","balance":1000.00,"currency":"USD"}
```

---

## 🔍 Troubleshooting

### **Issue: 500 Internal Server Error on POST /events**

**Solution**: Check that you're using the correct field names:
- ✅ `type` (not `eventType`)
- ✅ `type` must be `CREDIT` or `DEBIT` (not `DEPOSIT`, `WITHDRAWAL`, etc.)
- ✅ All required fields present: `eventId`, `accountId`, `type`, `amount`, `currency`, `eventTimestamp`

### **Issue: 404 Not Found on /events**

**Solution**: Make sure you're using correct endpoint paths:
- ✅ `/events` (GET or POST)
- ✅ NOT `/events/health` (use `/health` instead)

### **Issue: SERVICE_UNAVAILABLE with UnknownHostException**

**Solution**: This has been fixed. Account Service URL is now `localhost:8081` instead of `account-service:8081`

### **Issue: Port Already in Use**

**Solution**: Kill the process using the port:
```bash
# For port 8080 (Event Gateway)
Get-NetTCPConnection -LocalPort 8080 | Stop-Process -Force

# For port 8081 (Account Service)
Get-NetTCPConnection -LocalPort 8081 | Stop-Process -Force
```

---

## 🚀 Quick Testing Commands

```bash
# Test everything quickly
curl http://localhost:8081/health
curl http://localhost:8080/health

# Create event
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId":"evt-test-'$(date +%s)'",
    "accountId":"ACC001",
    "type":"CREDIT",
    "amount":1000,
    "currency":"USD",
    "eventTimestamp":"'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'"
  }'

# View results
curl http://localhost:8081/accounts/ACC001/balance
curl "http://localhost:8080/events?account=ACC001"
```

---

## 📈 What's Working

✅ **Health Checks** - Both services report UP  
✅ **Event Creation** - Events created successfully  
✅ **Balance Calculation** - Balances calculated correctly  
✅ **Transaction Tracking** - Transactions tracked and stored  
✅ **Idempotency** - Duplicate event IDs handled correctly  
✅ **Service-to-Service Communication** - Gateway calls Account Service successfully  
✅ **Distributed Tracing** - Trace IDs propagated between services  
✅ **Structured Logging** - JSON logs with trace IDs  
✅ **Error Handling** - Centralized error handling with structured responses  

---

## 📝 Key Validation Rules

| Field | Rule |
|-------|------|
| `eventId` | Required, unique identifier |
| `accountId` | Required, account identifier |
| `type` | Required, must be "CREDIT" or "DEBIT" |
| `amount` | Required, must be > 0 |
| `currency` | Required, e.g., "USD" |
| `eventTimestamp` | Required, ISO 8601 format |
| `description` | Optional, free text |
| `metadata` | Optional, JSON object |

---

**All services are operational and ready for production testing!** 🎉


