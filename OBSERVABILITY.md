# Observability Implementation Guide

This document details the observability implementation in the EventLedger project, including distributed tracing, structured logging, health checks, and metrics.

## Table of Contents
1. [Distributed Tracing](#distributed-tracing)
2. [Structured JSON Logging](#structured-json-logging)
3. [Health Check Endpoints](#health-check-endpoints)
4. [Custom Metrics](#custom-metrics)
5. [Monitoring Dashboard Examples](#monitoring-dashboard-examples)

---

## Distributed Tracing

### Overview

Distributed tracing enables tracking of requests as they flow through multiple services. The EventLedger uses **Spring Cloud Sleuth** with **OpenTelemetry** standards.

### Architecture

```
┌─────────────────────────────────────────────────┐
│         Client Request                          │
└────────────────┬────────────────────────────────┘
                 │ Generate Trace ID
                 │ X-B3-TraceId: 4883b868de-ab14-bca0
                 │ X-B3-SpanId: c8e3ca9e4a7b
                 ▼
        ┌────────────────────┐
        │  Event Gateway     │
        │  (8080)            │
        │                    │
        │ - Log Request      │
        │ - Log Response     │ Propagate Trace ID
        └────────┬───────────┘  via HTTP Headers
                 │
                 │ X-B3-TraceId: 4883b868de-ab14-bca0
                 │ (Same Trace ID)
                 ▼
        ┌────────────────────┐
        │ Account Service    │
        │  (8081)            │
        │                    │
        │ - Log Transaction  │
        │   (Same Trace ID)  │
        └────────────────────┘
```

### How Trace Propagation Works

1. **Trace ID Generation**: Gateway generates a 128-bit trace ID on incoming request
2. **Header Propagation**: Spring Cloud Sleuth automatically adds trace ID to HTTP headers:
   - `X-B3-TraceId`: Unique trace identifier
   - `X-B3-SpanId`: Unique span identifier
   - `X-B3-ParentSpanId`: Parent span identifier
3. **Downstream Logging**: Account Service receives the headers and logs with the same trace ID
4. **Correlation**: Both services use the same trace ID for log correlation

### Configuration

**File**: `application.properties` (both services)

```properties
# Sleuth Configuration (Distributed Tracing)
spring.sleuth.traceId128=true                    # Use 128-bit trace IDs (recommended)
spring.sleuth.propagation.type=w3c,b3single    # Support both W3C and B3 formats
spring.sleuth.sampler.probability=1.0           # Sample 100% of requests
```

### Automatic Instrumentation

Spring Cloud Sleuth automatically instruments:
- **HTTP Clients**: RestTemplate, WebClient
- **HTTP Servers**: Spring DispatcherServlet
- **Async Tasks**: @Async, ThreadPoolTaskExecutor
- **Database Operations**: JDBC, JPA
- **Messaging**: Spring Cloud Stream

### Manual Trace ID Access

To access the trace ID in code:

```java
import org.springframework.cloud.sleuth.Tracer;

@Component
public class MyComponent {
    private final Tracer tracer;

    public MyComponent(Tracer tracer) {
        this.tracer = tracer;
    }

    public void doSomething() {
        String traceId = tracer.currentSpan().context().traceId();
        String spanId = tracer.currentSpan().context().spanId();
        logger.info("Current trace: {} span: {}", traceId, spanId);
    }
}
```

### Sleuth MDC Integration

Sleuth automatically adds trace information to Mapped Diagnostic Context (MDC):

```java
// In logback.xml or code
import org.slf4j.MDC;

// Available MDC keys:
MDC.get("traceId")  // Trace ID
MDC.get("spanId")   // Span ID
MDC.get("baggage-userId")  // Custom baggage
```

---

## Structured JSON Logging

### Overview

Structured JSON logging provides machine-readable log output suitable for log aggregation and analysis.

### Configuration Files

**File**: `logback.xml` (both services)

Uses `logstash-logback-encoder` to format logs as JSON:

```xml
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <customFields>{"service":"account-service"}</customFields>
        <includeContext>true</includeContext>
        <fieldNames>
            <!-- Field mapping configuration -->
        </fieldNames>
    </encoder>
</appender>
```

### Log Output Format

Each log entry includes:

```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "@version": "1",
  "service": "event-gateway",
  "traceId": "4883b868de-ab14-bca0",
  "spanId": "c8e3ca9e4a7b",
  "level": "INFO",
  "logger_name": "com.eventledger.eventgateway.controller.EventGatewayController",
  "message": "Event created successfully for account: ACC001",
  "thread_name": "http-nio-8080-exec-1",
  "tags": [],
  "mdc": {
    "traceId": "4883b868de-ab14-bca0",
    "spanId": "c8e3ca9e4a7b"
  }
}
```

### Log Field Descriptions

| Field | Description |
|-------|-------------|
| `@timestamp` | ISO 8601 timestamp |
| `@version` | Logstash format version |
| `service` | Service name (from config) |
| `traceId` | Distributed trace ID (128-bit) |
| `spanId` | Span ID for this operation |
| `level` | Log level (DEBUG, INFO, WARN, ERROR) |
| `logger_name` | Full class name of logger |
| `message` | Log message |
| `thread_name` | Thread that logged the message |
| `mdc` | Mapped Diagnostic Context |

### Logger Configuration

**Logger Levels** (from `logback.xml`):

```xml
<!-- Debug level for application code -->
<logger name="com.eventledger" level="DEBUG"/>

<!-- Info level for Spring framework -->
<logger name="org.springframework" level="INFO"/>

<!-- Debug level for Sleuth to see trace details -->
<logger name="org.springframework.cloud.sleuth" level="DEBUG"/>
```

### Log Examples

**Transaction Creation**:
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "service": "account-service",
  "traceId": "4883b868de-ab14-bca0",
  "level": "INFO",
  "message": "Transaction created successfully for account: ACC001"
}
```

**Error Logging**:
```json
{
  "timestamp": "2024-01-15T10:30:46.456Z",
  "service": "event-gateway",
  "traceId": "4883b868de-ab14-bca0",
  "level": "ERROR",
  "message": "Error creating transaction",
  "exception": "AccountServiceException: Service unavailable"
}
```

### Log Filtering Queries

Example queries for log aggregation systems (ELK, Splunk, etc.):

```
# Find all errors for a specific trace
service:"event-gateway" AND traceId:"4883b868de-ab14-bca0" AND level:"ERROR"

# Find slow operations (duration > 1000ms)
service:"account-service" AND logger_name:"*Controller" AND duration_ms > 1000

# Find service-to-service calls with trace ID
traceId:* AND message:*"Account Service"*

# Find all transactions for an account
service:* AND traceId:* AND message:*"ACC001"*
```

---

## Health Check Endpoints

### Overview

Health check endpoints provide real-time diagnostics about service status.

### Endpoints

#### Account Service Health
**Endpoint**: `GET /health`
**Port**: 8081

```bash
curl http://localhost:8081/health
```

**Response**:
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

#### Event Gateway Health
**Endpoint**: `GET /health`
**Port**: 8080

```bash
curl http://localhost:8080/health
```

**Response**:
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

### Actuator Endpoints

Spring Boot Actuator provides additional endpoints:

**Available Endpoints**:

```
GET /actuator/health        - Health status
GET /actuator/metrics       - List all metrics
GET /actuator/info          - Application info
GET /actuator/health/db     - Database health
GET /actuator/health/ping   - Ping check
```

### Health Check in Docker

Docker Compose includes health checks:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
  interval: 30s      # Check every 30 seconds
  timeout: 10s       # Timeout after 10 seconds
  retries: 3         # Fail after 3 retries
  start_period: 10s  # Grace period before first check
```

### Implementation Details

**File**: `HealthController.java` (both services)

```java
@GetMapping("/health")
public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> healthResponse = new HashMap<>();
    healthResponse.put("status", "UP");
    healthResponse.put("service", "account-service");
    healthResponse.put("timestamp", System.currentTimeMillis());
    
    // Check database connectivity
    Map<String, String> dbStatus = checkDatabaseHealth();
    healthResponse.put("database", dbStatus);
    
    return ResponseEntity.ok(healthResponse);
}

private Map<String, String> checkDatabaseHealth() {
    // Test database connection
    try {
        Connection connection = dataSource.getConnection();
        if (connection != null && !connection.isClosed()) {
            dbStatus.put("status", "UP");
        }
    } catch (Exception e) {
        dbStatus.put("status", "DOWN");
    }
    return dbStatus;
}
```

---

## Custom Metrics

### Overview

Custom metrics track application-specific measurements like transaction counts, error rates, and operation latencies.

### Metrics Architecture

```
┌──────────────────────────────┐
│    Business Operations       │
│  (Controller, Service)       │
└────────────┬─────────────────┘
             │
             ▼
┌──────────────────────────────┐
│  Metrics Collection          │
│  (AccountServiceMetrics)     │
├──────────────────────────────┤
│ - Counters                   │
│ - Timers                     │
│ - Gauges                     │
└────────────┬─────────────────┘
             │
             ▼
┌──────────────────────────────┐
│  Micrometer Registry         │
└────────────┬─────────────────┘
             │
             ▼
┌──────────────────────────────┐
│  Expose via /actuator/metrics│
└──────────────────────────────┘
```

### Available Metrics

#### Account Service Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `account.transactions.created` | Counter | Total transactions created |
| `account.balance.checks` | Counter | Total balance checks performed |
| `account.errors.total` | Counter | Total errors encountered |
| `account.transaction.duration` | Timer | Time to create transaction (p50, p95, p99) |
| `account.balance.check.duration` | Timer | Time to check balance (p50, p95, p99) |
| `account.active.transactions` | Gauge | Current active transactions |

#### Event Gateway Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `events.created.total` | Counter | Total events created |
| `events.retrieval.total` | Counter | Total event retrievals |
| `events.errors.total` | Counter | Total event errors |
| `downstream.service.errors.total` | Counter | Errors from account service |
| `events.creation.duration` | Timer | Event creation latency |
| `events.retrieval.duration` | Timer | Event retrieval latency |
| `events.active.count` | Gauge | Currently active events |

### Metrics Collection Implementation

**File**: `AccountServiceMetrics.java`

```java
@Component
public class AccountServiceMetrics {
    private final Counter transactionCounter;
    private final Timer transactionTimer;
    private final AtomicInteger activeTransactions;

    public AccountServiceMetrics(MeterRegistry meterRegistry) {
        this.transactionCounter = Counter.builder("account.transactions.created")
            .description("Total number of transactions created")
            .register(meterRegistry);
        
        this.transactionTimer = Timer.builder("account.transaction.duration")
            .description("Time taken to create a transaction")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
        
        this.activeTransactions = meterRegistry.gauge("account.active.transactions",
            new AtomicInteger(0),
            AtomicInteger::get);
    }

    public void recordTransactionCreated() {
        transactionCounter.increment();
    }

    public Timer.Sample startTransactionTimer() {
        return Timer.start(transactionTimer);
    }
}
```

### Viewing Metrics

**List All Metrics**:
```bash
curl http://localhost:8080/actuator/metrics
```

**Response**:
```json
{
  "names": [
    "account.transactions.created",
    "account.balance.checks",
    "events.created.total",
    "downstream.service.errors.total",
    "jvm.memory.used",
    "process.uptime",
    ...
  ]
}
```

**View Specific Metric**:
```bash
curl http://localhost:8080/actuator/metrics/events.created.total
```

**Response**:
```json
{
  "name": "events.created.total",
  "description": "Total number of events created",
  "baseUnit": null,
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42
    }
  ],
  "availableTags": []
}
```

**View Timer with Percentiles**:
```bash
curl http://localhost:8081/actuator/metrics/account.transaction.duration
```

**Response**:
```json
{
  "name": "account.transaction.duration",
  "description": "Time taken to create a transaction",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 5.234
    },
    {
      "statistic": "MAX",
      "value": 0.456
    }
  ],
  "availableTags": []
}
```

### Metrics in Controller Integration

**File**: `AccountController.java`

```java
@PostMapping("/{accountId}/transactions")
public ResponseEntity<TransactionResponse> createTransaction(
        @PathVariable String accountId,
        @RequestBody TransactionRequest request) {
    Timer.Sample sample = metrics.startTransactionTimer();
    metrics.incrementActiveTransactions();
    
    try {
        TransactionResponse response = accountService.createTransaction(request);
        metrics.recordTransactionCreated();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (Exception e) {
        metrics.recordError();
        throw e;
    } finally {
        sample.stop(metrics.startTransactionTimer());
        metrics.decrementActiveTransactions();
    }
}
```

### Metrics Export Formats

Micrometer supports exporting metrics to multiple backends:

**Configured in docker-compose.yml**:
```properties
management.metrics.export.simple.enabled=true
```

### Performance Impact

- **Negligible overhead**: Metrics collection adds < 1ms per operation
- **Async processing**: Metrics are collected asynchronously
- **Memory efficient**: Counters and timers use minimal memory

---

## Monitoring Dashboard Examples

### Example 1: Trace a Request End-to-End

```bash
# 1. Make a request to gateway
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC001",
    "eventType": "DEPOSIT",
    "amount": 1000,
    "description": "Test deposit"
  }'

# 2. Watch gateway logs for trace ID
docker-compose logs event-gateway | grep "traceId"

# 3. Find same trace ID in account service logs
docker-compose logs account-service | grep "4883b868de-ab14-bca0"

# 4. Observe the complete flow with same trace ID
```

### Example 2: Monitor Error Rates

```bash
# Get initial error count
curl http://localhost:8081/actuator/metrics/account.errors.total

# Make a request that causes an error
curl -X POST http://localhost:8081/accounts/INVALID/transactions \
  -H "Content-Type: application/json" \
  -d '{"amount": -100}'

# Get new error count
curl http://localhost:8081/actuator/metrics/account.errors.total
```

### Example 3: Measure Operation Latency

```bash
# View transaction creation latency distribution
curl http://localhost:8081/actuator/metrics/account.transaction.duration

# Response shows p50, p95, p99 latencies
```

### Example 4: Health Status Dashboard

```bash
#!/bin/bash
while true; do
  echo "=== Event Gateway Health ==="
  curl -s http://localhost:8080/health | jq '.database.status'
  
  echo "=== Account Service Health ==="
  curl -s http://localhost:8081/health | jq '.database.status'
  
  sleep 5
done
```

---

## Integration with External Systems

### ELK Stack Integration

Example Filebeat configuration to ship logs to Elasticsearch:

```yaml
filebeat.inputs:
- type: container
  paths:
    - '/var/lib/docker/containers/*/*.log'
  processors:
    - decode_json_fields:
        fields: ["message"]
        target: "json"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
```

### Prometheus Integration

To add Prometheus support:

1. Add dependency to pom.xml:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. Metrics available at:
```
GET /actuator/prometheus
```

3. Prometheus scrape config:
```yaml
scrape_configs:
  - job_name: 'event-ledger'
    static_configs:
      - targets: ['localhost:8080', 'localhost:8081']
    metrics_path: '/actuator/prometheus'
```

### Jaeger Integration

To add Jaeger for trace visualization:

1. Add dependency:
```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-jaeger</artifactId>
</dependency>
```

2. Configure in application.properties:
```properties
otel.exporter.jaeger.endpoint=http://jaeger:14268/api/traces
```

---

## Best Practices

### Logging Best Practices

✓ **DO:**
- Log at appropriate levels (DEBUG for details, INFO for important events)
- Include trace ID in all logs
- Use structured JSON logging
- Log at service boundaries (entry/exit)

✗ **DON'T:**
- Log sensitive data (passwords, tokens)
- Use string concatenation for logs (use parameters)
- Log at DEBUG level for high-volume operations

### Metrics Best Practices

✓ **DO:**
- Use meaningful metric names
- Include units in descriptions
- Record both success and error counts
- Monitor latency with percentiles

✗ **DON'T:**
- Create high-cardinality metrics
- Record unbounded metrics
- Forget to handle exceptions in metrics

### Tracing Best Practices

✓ **DO:**
- Use 128-bit trace IDs
- Propagate trace IDs across services
- Sample appropriately (100% for production debugging)
- Correlate logs with traces

✗ **DON'T:**
- Lose trace IDs in async operations
- Sample too low for debugging
- Expose trace IDs in external APIs

---

## Troubleshooting

### Trace IDs Not Appearing in Logs

**Symptom**: Logs don't include traceId field

**Solution**:
1. Check `logback.xml` is in `src/main/resources/`
2. Verify `spring.sleuth.sampler.probability=1.0`
3. Check for log level (DEBUG or higher needed)
4. Restart services

### Metrics Not Showing

**Symptom**: `/actuator/metrics` returns empty

**Solution**:
1. Verify `management.endpoints.web.exposure.include=health,metrics`
2. Check metrics are being recorded in code
3. Make some requests to generate metrics
4. Restart services

### High Memory Usage

**Symptom**: Service uses excessive memory

**Solution**:
1. Reduce metric cardinality
2. Increase JVM heap in Dockerfile
3. Check for metric memory leaks
4. Monitor with: `curl http://localhost:8080/actuator/metrics/jvm.memory.used`

---

## Conclusion

The EventLedger project includes comprehensive observability:

- **Distributed Tracing**: Trace requests across services
- **Structured Logging**: Machine-readable JSON logs
- **Health Checks**: Real-time service status
- **Custom Metrics**: Application-specific measurements

These features enable effective monitoring, debugging, and optimization of the distributed system.

