# EventLedger - Distributed Tracing & Observability Implementation

This document provides an overview of the distributed tracing and observability features implemented in the EventLedger project.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Distributed Tracing Implementation](#distributed-tracing-implementation)
3. [Observability Features](#observability-features)
4. [Docker Deployment](#docker-deployment)
5. [Testing the System](#testing-the-system)
6. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites

- Java 8+
- Maven 3.6+
- Docker & Docker Compose (for containerized deployment)

### Local Deployment (without Docker)

```bash
# Terminal 1: Start Account Service
cd account-service
mvn clean install
mvn spring-boot:run

# Terminal 2: Start Event Gateway
cd event-gateway
mvn clean install
mvn spring-boot:run
```

**Services will be available at:**
- Account Service: http://localhost:8081
- Event Gateway: http://localhost:8080

### Docker Deployment

```bash
# Build and start all services
docker-compose up --build

# Services will be available at:
# - Account Service: http://localhost:8081
# - Event Gateway: http://localhost:8080
```

See [DOCKER_SETUP.md](./DOCKER_SETUP.md) for detailed Docker instructions.

---

## Distributed Tracing Implementation

### Overview

The EventLedger implements **end-to-end distributed tracing** using:
- **Spring Cloud Sleuth**: Automatic trace ID management and propagation
- **OpenTelemetry**: Standard tracing API
- **W3C & B3 Headers**: Standard trace header formats

### How It Works

```
Client Request → Event Gateway → Account Service → Response
     ↓                  ↓                 ↓
 Generate        Propagate         Log with Same
 Trace ID        Trace ID          Trace ID
```

### Trace ID Format

- **128-bit trace IDs**: More unique identifiers for large-scale systems
- **Header Format**: `X-B3-TraceId: 4883b868de-ab14-bca0` (W3C format)
- **Propagation**: Automatic across HTTP calls via RestTemplate

### Example Trace Flow

```bash
# 1. Make a request
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC001",
    "eventType": "DEPOSIT",
    "amount": 1000,
    "description": "Initial deposit"
  }'

# 2. Trace ID is generated at Gateway:
# LOG: traceId=4883b868de-ab14-bca0 - Event received

# 3. Trace ID is propagated to Account Service:
# LOG: traceId=4883b868de-ab14-bca0 - Transaction created

# 4. Both logs are correlated by the same trace ID
```

### Configuration

**Enabled in**: `application.properties` (both services)

```properties
# 128-bit trace IDs (more unique)
spring.sleuth.traceId128=true

# Support multiple header formats
spring.sleuth.propagation.type=w3c,b3single

# Sample 100% of requests
spring.sleuth.sampler.probability=1.0
```

### MDC (Mapped Diagnostic Context) Integration

Sleuth automatically adds trace information to SLF4J MDC:

```java
// Available in logs:
MDC.get("traceId")    // Trace ID
MDC.get("spanId")     // Span ID
```

---

## Observability Features

### 1. Structured JSON Logging

All logs are output in structured JSON format for easy parsing and analysis.

**Example Log Entry:**
```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "service": "event-gateway",
  "traceId": "4883b868de-ab14-bca0",
  "spanId": "c8e3ca9e4a7b",
  "level": "INFO",
  "logger_name": "com.eventledger.eventgateway.controller.EventGatewayController",
  "message": "Event created successfully for account: ACC001",
  "thread_name": "http-nio-8080-exec-1"
}
```

**Configured in**: `src/main/resources/logback.xml`

**Key Features:**
- Automatic inclusion of trace ID and span ID
- Service name identification
- Timestamp in ISO 8601 format
- Async processing for performance

### 2. Health Check Endpoints

Both services provide health check endpoints with diagnostics.

**Account Service:**
```bash
curl http://localhost:8081/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "account-service",
  "timestamp": 1705315845123,
  "database": {
    "status": "UP",
    "connection": "OK"
  }
}
```

**Event Gateway:**
```bash
curl http://localhost:8080/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "event-gateway",
  "timestamp": 1705315845123,
  "database": {
    "status": "UP",
    "connection": "OK"
  },
  "downstream": {
    "service": "account-service",
    "url": "http://account-service:8081/health",
    "status": "EXPECTED"
  }
}
```

### 3. Custom Metrics

Both services track application-specific metrics for monitoring.

#### Account Service Metrics

```
GET /actuator/metrics

{
  "names": [
    "account.transactions.created",      // Counter
    "account.balance.checks",            // Counter
    "account.errors.total",              // Counter
    "account.transaction.duration",      // Timer
    "account.balance.check.duration",    // Timer
    "account.active.transactions"        // Gauge
  ]
}
```

#### Event Gateway Metrics

```
GET /actuator/metrics

{
  "names": [
    "events.created.total",              // Counter
    "events.retrieval.total",            // Counter
    "events.errors.total",               // Counter
    "downstream.service.errors.total",   // Counter
    "events.creation.duration",          // Timer
    "events.retrieval.duration",         // Timer
    "events.active.count"                // Gauge
  ]
}
```

#### Viewing Metric Details

```bash
# View specific metric
curl http://localhost:8080/actuator/metrics/events.created.total

# Response:
{
  "name": "events.created.total",
  "description": "Total number of events created",
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42
    }
  ]
}

# View timer with percentiles
curl http://localhost:8081/actuator/metrics/account.transaction.duration

# Response:
{
  "name": "account.transaction.duration",
  "measurements": [
    {"statistic": "COUNT", "value": 42},
    {"statistic": "TOTAL_TIME", "value": 5.234},
    {"statistic": "MAX", "value": 0.456}
  ]
}
```

---

## Docker Deployment

### Prerequisites

- Docker (v20.10+)
- Docker Compose (v1.29+)

### Quick Start

```bash
# Navigate to project root
cd EventLedger

# Build and start services
docker-compose up --build

# Verify services are running
docker-compose ps
```

### Service Architecture

```yaml
Services:
  - account-service (port 8081)
  - event-gateway (port 8080)

Network:
  - event-ledger-network (bridge)

Health Checks:
  - Automatic every 30 seconds
  - Grace period: 10 seconds
  - Retries: 3
```

### Dockerfile Features

- **Multi-stage build**: Optimized image size
- **Health checks**: Automatic container health monitoring
- **JVM tuning**: G1 Garbage Collector, 256MB heap
- **Curl support**: For health check endpoints

### Docker Compose Features

- **Service dependency**: Event Gateway waits for Account Service
- **Environment configuration**: All properties via environment variables
- **Network isolation**: Services communicate via Docker bridge network
- **Volume management**: H2 in-memory databases (recreated on restart)

See [DOCKER_SETUP.md](./DOCKER_SETUP.md) for comprehensive Docker guide.

---

## Testing the System

### Test 1: Basic Request Flow

```bash
# 1. Create an event (triggers trace propagation)
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC001",
    "eventType": "DEPOSIT",
    "amount": 1000,
    "description": "Initial deposit"
  }'

# 2. View logs to see trace ID propagation
docker-compose logs event-gateway
docker-compose logs account-service

# Expected: Both logs contain the same traceId
```

### Test 2: Trace ID Correlation

```bash
# 1. Make a request
RESPONSE=$(curl -s -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC002",
    "eventType": "WITHDRAWAL",
    "amount": 500,
    "description": "Test withdrawal"
  }')

# 2. Check gateway logs
GATEWAY_LOG=$(docker-compose logs event-gateway | tail -20)
echo "Gateway Log: $GATEWAY_LOG"

# 3. Check account service logs  
ACCOUNT_LOG=$(docker-compose logs account-service | tail -20)
echo "Account Log: $ACCOUNT_LOG"

# Expected: Same traceId in both logs
```

### Test 3: Health Checks

```bash
# Account Service Health
curl -s http://localhost:8081/health | jq .

# Event Gateway Health
curl -s http://localhost:8080/health | jq .

# Expected: Both return status: "UP"
```

### Test 4: Metrics

```bash
# Get all metrics from Account Service
curl -s http://localhost:8081/actuator/metrics | jq .names

# Get specific metric
curl -s http://localhost:8081/actuator/metrics/account.transactions.created | jq .

# Get all metrics from Event Gateway
curl -s http://localhost:8080/actuator/metrics | jq .names
```

### Test 5: Error Rate Tracking

```bash
# 1. Get initial error count
INITIAL=$(curl -s http://localhost:8080/actuator/metrics/events.errors.total)
echo "Initial errors: $INITIAL"

# 2. Make multiple requests (some will fail)
for i in {1..5}; do
  curl -s -X POST http://localhost:8080/events \
    -H "Content-Type: application/json" \
    -d '{"accountId":"", "eventType":"TEST"}' > /dev/null
done

# 3. Get new error count
FINAL=$(curl -s http://localhost:8080/actuator/metrics/events.errors.total)
echo "Final errors: $FINAL"

# Expected: Error count increased
```

### Test 6: Latency Measurement

```bash
# Create multiple transactions to build latency data
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/events \
    -H "Content-Type: application/json" \
    -d "{
      \"accountId\": \"ACC$(printf %03d $i)\",
      \"eventType\": \"DEPOSIT\",
      \"amount\": $((i * 100)),
      \"description\": \"Transaction $i\"
    }" > /dev/null
done

# View transaction creation latency percentiles
curl -s http://localhost:8081/actuator/metrics/account.transaction.duration | jq .
```

---

## Key Files and Locations

### Tracing Configuration

- **Account Service**: `account-service/src/main/resources/application.properties`
- **Event Gateway**: `event-gateway/src/main/resources/application.properties`
- **Logback Config**: 
  - `account-service/src/main/resources/logback.xml`
  - `event-gateway/src/main/resources/logback.xml`

### Metrics Implementation

- **Account Service**: `account-service/src/main/java/.../metrics/AccountServiceMetrics.java`
- **Event Gateway**: `event-gateway/src/main/java/.../metrics/EventGatewayMetrics.java`

### Health Checks

- **Account Service**: `account-service/src/main/java/.../controller/HealthController.java`
- **Event Gateway**: `event-gateway/src/main/java/.../controller/EventGatewayController.java`

### Docker Files

- **Account Service Dockerfile**: `account-service/Dockerfile`
- **Event Gateway Dockerfile**: `event-gateway/Dockerfile`
- **Docker Compose**: `docker-compose.yml`

### Dependencies

- **Root POM**: `pom.xml` (parent project with versions)
- **Account Service POM**: `account-service/pom.xml`
- **Event Gateway POM**: `event-gateway/pom.xml`

---

## Troubleshooting

### Trace IDs Not Showing in Logs

**Problem**: Logs don't contain `traceId` field

**Solution**:
1. Verify `logback.xml` is in `src/main/resources/`
2. Check `spring.sleuth.sampler.probability=1.0` in properties
3. Restart services: `docker-compose restart`
4. Check logger levels (must be DEBUG or higher)

### Metrics Not Appearing

**Problem**: `/actuator/metrics` returns empty list

**Solution**:
1. Verify `management.endpoints.web.exposure.include=health,metrics,info`
2. Make some API requests to generate metrics
3. Check that MetricsComponent is being injected into controllers
4. View logs for "Metrics initialized" message

### Health Check Failing

**Problem**: Health endpoint returns DOWN status

**Solution**:
1. Check database connectivity: `curl -v http://localhost:8081/health`
2. View detailed error: Add `management.endpoint.health.show-details=always`
3. Check logs: `docker-compose logs account-service`
4. Verify H2 database is initialized

### Service to Service Communication Failing

**Problem**: Event Gateway can't reach Account Service

**Solution**:
1. Verify services are on same network: `docker network ls`
2. Check service name in AccountServiceClient URL (should be `http://account-service:8081`)
3. Verify Account Service is running: `docker-compose logs account-service`
4. Test connectivity: `docker exec event-gateway curl http://account-service:8081/health`

### High Memory Usage

**Problem**: Docker containers using excessive memory

**Solution**:
1. Check JVM settings in Dockerfile: `-Xmx256m`
2. Increase heap size if needed: `-Xmx512m`
3. Check for metric memory leaks: `curl http://localhost:8080/actuator/metrics/jvm.memory.used`
4. Monitor with: `docker stats`

---

## Performance Metrics

### Expected Performance

- **Transaction Creation**: < 100ms (p95)
- **Balance Check**: < 50ms (p95)
- **Event Retrieval**: < 100ms (p95)
- **Trace Overhead**: < 1ms per request
- **Memory Usage**: ~200-300MB per service

### Monitoring Performance

```bash
# View transaction latency distribution
curl http://localhost:8081/actuator/metrics/account.transaction.duration

# View current memory usage
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# View active transactions
curl http://localhost:8081/actuator/metrics/account.active.transactions
```

---

## Integration with External Systems

### ELK Stack (Elasticsearch, Logstash, Kibana)

The structured JSON logging format integrates seamlessly with ELK:

1. **Filebeat** ships logs to **Elasticsearch**
2. **Kibana** queries and visualizes logs
3. **Trace IDs** enable correlation across services

Example query in Kibana:
```
traceId:"4883b868de-ab14-bca0" AND (service:"event-gateway" OR service:"account-service")
```

### Prometheus & Grafana

Add Prometheus support for metrics visualization:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Metrics available at: `GET /actuator/prometheus`

### Jaeger (Distributed Tracing UI)

Add Jaeger for visual trace exploration:

```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
</dependency>
```

---

## Best Practices

### Logging
- ✓ Always include trace ID in logs
- ✓ Use appropriate log levels
- ✓ Log at service boundaries
- ✗ Don't log sensitive information

### Metrics
- ✓ Use meaningful names
- ✓ Track both success and error counts
- ✓ Monitor latency with percentiles
- ✗ Don't create high-cardinality metrics

### Tracing
- ✓ Enable 100% sampling for debugging
- ✓ Propagate trace IDs across services
- ✓ Use consistent header formats
- ✗ Don't lose trace IDs in async operations

---

## Documentation References

- **Distributed Tracing Details**: See [OBSERVABILITY.md](./OBSERVABILITY.md)
- **Docker Setup Guide**: See [DOCKER_SETUP.md](./DOCKER_SETUP.md)
- **Project Overview**: See [PROJECT_SUMMARY.md](./PROJECT_SUMMARY.md)

---

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review logs: `docker-compose logs -f`
3. Check trace IDs in logs for correlation
4. Verify health endpoints are responding
5. Monitor metrics for anomalies

---

## Summary

The EventLedger project now includes:

✅ **Distributed Tracing**: End-to-end request tracking across services
✅ **Structured Logging**: JSON-formatted logs with trace IDs
✅ **Health Checks**: Service status monitoring with diagnostics
✅ **Custom Metrics**: Application-specific measurements and analytics
✅ **Docker Support**: Complete containerized deployment solution

These features enable comprehensive observability and monitoring of the distributed EventLedger system.

