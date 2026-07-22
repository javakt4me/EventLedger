# Docker Setup Guide for EventLedger

This guide explains how to build and run the EventLedger project using Docker Compose.

## Prerequisites

- Docker (version 20.10+)
- Docker Compose (version 1.29+)
- Git (to clone the repository)

### Install Docker

**Windows:**
- Download Docker Desktop from https://www.docker.com/products/docker-desktop
- Follow the installation instructions

**Mac:**
- Download Docker Desktop from https://www.docker.com/products/docker-desktop
- Follow the installation instructions

**Linux:**
```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose
sudo usermod -aG docker $USER
```

## Quick Start with Docker Compose

### 1. Navigate to Project Root
```bash
cd EventLedger
```

### 2. Build and Start Services
```bash
docker-compose up --build
```

This command will:
- Build Docker images for both services
- Start the account-service (port 8081)
- Start the event-gateway (port 8080)
- Ensure both services are healthy before marking as ready

### 3. Verify Services are Running
```bash
docker-compose ps
```

Expected output:
```
NAME                    STATUS
account-service         Up (healthy)
event-gateway           Up (healthy)
```

## Using the Services

### Health Checks

**Account Service Health:**
```bash
curl http://localhost:8081/health
```

**Event Gateway Health:**
```bash
curl http://localhost:8080/health
```

Expected response:
```json
{
  "status": "UP",
  "service": "account-service",
  "timestamp": 1234567890,
  "database": {
    "status": "UP",
    "connection": "OK"
  }
}
```

### View Metrics

**Account Service Metrics:**
```bash
curl http://localhost:8081/actuator/metrics
```

**Event Gateway Metrics:**
```bash
curl http://localhost:8080/actuator/metrics
```

Available metrics:
- `account.transactions.created` - Total transactions created
- `account.balance.checks` - Total balance checks
- `account.errors.total` - Total errors
- `events.created.total` - Total events created
- `events.retrieval.total` - Total event retrievals
- `downstream.service.errors.total` - Downstream service errors

### View Specific Metric
```bash
curl http://localhost:8080/actuator/metrics/events.created.total
```

## Distributed Tracing

The EventLedger system uses **Spring Cloud Sleuth** with **OpenTelemetry** for distributed tracing.

### Trace ID Propagation

When you make a request through the event-gateway to account-service:

1. **Event Gateway** generates or receives a trace ID
2. **Trace ID** is propagated via HTTP headers (W3C format)
3. **Account Service** logs using the same trace ID
4. **Both services** include the trace ID in structured JSON logs

### Viewing Trace IDs in Logs

Check the logs of both services to see trace IDs:

```bash
# View gateway logs
docker-compose logs event-gateway

# View account service logs
docker-compose logs account-service
```

Log entries will include:
```json
{
  "timestamp": "2024-01-15 10:30:45.123",
  "service": "event-gateway",
  "traceId": "4883b868de-ab14-bca0",
  "spanId": "c8e3ca9e4a7b",
  "level": "INFO",
  "message": "Event created successfully"
}
```

## Structured JSON Logging

Both services output logs in structured JSON format via `logback.xml` configuration:

- **timestamp**: ISO 8601 format
- **service**: Service name (account-service or event-gateway)
- **traceId**: Distributed trace ID (128-bit)
- **spanId**: Span ID for OpenTelemetry
- **level**: Log level (DEBUG, INFO, WARN, ERROR)
- **logger_name**: Full logger class name
- **message**: Log message
- **mdc**: Mapped Diagnostic Context

### Sample Structured Log Entry
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

## API Examples

### Create an Event (with Trace Propagation)
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC001",
    "eventType": "DEPOSIT",
    "amount": 1000,
    "description": "Initial deposit"
  }'
```

This request will:
1. Generate a trace ID at the gateway
2. Log with trace ID at the gateway
3. Propagate trace ID to account-service
4. Log with the same trace ID at account-service
5. Return response with trace ID in logs

### Get Events
```bash
curl http://localhost:8080/events?account=ACC001
```

### Get Account Balance
```bash
curl http://localhost:8081/accounts/ACC001/balance
```

## Stopping Services

### Stop All Services
```bash
docker-compose down
```

### Stop Services but Keep Volumes
```bash
docker-compose down -v
```

### View Service Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f event-gateway
docker-compose logs -f account-service

# Last 50 lines
docker-compose logs --tail=50

# Follow real-time logs
docker-compose logs -f
```

## Environment Configuration

Both services are configured via environment variables in `docker-compose.yml`:

### Account Service Variables
- `SPRING_APPLICATION_NAME`: Service name (account-service)
- `SERVER_PORT`: Server port (8081)
- `SPRING_DATASOURCE_URL`: Database URL (H2 in-memory)
- `SPRING_SLEUTH_TRACEID128`: Use 128-bit trace IDs (true)
- `SPRING_SLEUTH_SAMPLER_PROBABILITY`: Sampling rate (1.0 = 100%)

### Event Gateway Variables
- Same as above, plus Resilience4j configuration for circuit breaker and retry patterns

## Troubleshooting

### Services Won't Start
```bash
# Check logs
docker-compose logs

# Ensure ports are available
netstat -an | grep -E '8080|8081'

# Rebuild images
docker-compose build --no-cache
docker-compose up
```

### Health Check Failing
```bash
# Check specific service logs
docker-compose logs account-service

# Test health endpoint manually
curl -v http://localhost:8081/health
```

### Trace IDs Not Showing
- Ensure `spring.sleuth.sampler.probability=1.0` is set
- Check that `logback.xml` is configured correctly
- Verify logs are being output in JSON format

### Service Can't Reach Another Service
- Ensure both services are on the same network
- Check service names match in configuration (e.g., `http://account-service:8081`)
- Verify `depends_on` constraints in docker-compose.yml

## Performance Tuning

The Dockerfiles include JVM tuning:
- `-Xmx256m`: Maximum heap size (256MB)
- `-XX:+UseG1GC`: G1 Garbage Collector for low latency

Adjust these in the Dockerfile `ENTRYPOINT` if needed:
```dockerfile
ENTRYPOINT ["java", "-Xmx512m", "-XX:+UseG1GC", "-jar", "app.jar"]
```

## Network Details

### Network: event-ledger-network
- Type: Bridge network
- Services connected: account-service, event-gateway
- Service discovery: Docker's internal DNS

Services can reach each other using service names:
- Account Service: `http://account-service:8081`
- Event Gateway: `http://event-gateway:8080`

## Health Check Configuration

Both services include automated health checks:
- **Interval**: 30 seconds
- **Timeout**: 10 seconds
- **Retries**: 3
- **Start Period**: 10 seconds (grace period before first check)

The `event-gateway` depends on `account-service` being healthy before starting.

## Monitoring

### View All Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### View Specific Metric Details
```bash
curl http://localhost:8080/actuator/metrics/events.created.total
curl http://localhost:8080/actuator/metrics/account.transactions.created
```

### Metrics Available
- `account.transactions.created`: Counter for created transactions
- `account.balance.checks`: Counter for balance inquiries
- `account.errors.total`: Counter for errors
- `events.created.total`: Counter for created events
- `events.retrieval.total`: Counter for retrieved events
- `downstream.service.errors.total`: Counter for downstream failures
- `*.duration`: Timers measuring operation latency (p50, p95, p99)

## Local Development (Without Docker)

If you prefer to run services locally without Docker:

### Prerequisites
- Java 8+
- Maven 3.6+

### Start Account Service
```bash
cd account-service
mvn spring-boot:run
```

### Start Event Gateway (in another terminal)
```bash
cd event-gateway
mvn spring-boot:run
```

### Services will be available at:
- Account Service: `http://localhost:8081`
- Event Gateway: `http://localhost:8080`

## Docker Build Tips

### Rebuild Without Cache
```bash
docker-compose build --no-cache
```

### Build Specific Service
```bash
docker-compose build account-service
```

### View Build Logs
```bash
docker-compose build --verbose
```

## Integration Testing

Both services are configured for integration testing with:
- H2 in-memory database
- Sleuth trace propagation
- Micrometer metrics collection
- Structured JSON logging

Test trace ID propagation:
```bash
# Terminal 1: Watch gateway logs
docker-compose logs -f event-gateway

# Terminal 2: Make request
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"accountId": "ACC001", "eventType": "DEPOSIT", "amount": 100, "description": "Test"}'

# Terminal 3: Watch account service logs
docker-compose logs -f account-service
```

You should see the same `traceId` in logs from both services.

## Additional Resources

- [Spring Cloud Sleuth Documentation](https://spring.io/projects/spring-cloud-sleuth)
- [OpenTelemetry Java Documentation](https://opentelemetry.io/docs/instrumentation/java/)
- [Micrometer Documentation](https://micrometer.io/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

