# Quick Reference Guide - Observability Features

## Getting Started

### Start Services Locally
```bash
# Terminal 1: Account Service
cd account-service && mvn spring-boot:run

# Terminal 2: Event Gateway
cd event-gateway && mvn spring-boot:run
```

### Start Services with Docker
```bash
docker-compose up --build
```

---

## Health Checks

```bash
# Account Service
curl http://localhost:8081/health

# Event Gateway
curl http://localhost:8080/health
```

---

## Distributed Tracing

### Make a Request (generates trace ID)
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC001",
    "eventType": "DEPOSIT",
    "amount": 1000,
    "description": "Test"
  }'
```

### View Trace ID in Logs
```bash
# Watch both services
docker-compose logs -f event-gateway &
docker-compose logs -f account-service &

# Look for traceId field in JSON logs
# Both services will show the same traceId
```

---

## Metrics

### List All Metrics
```bash
# Account Service
curl http://localhost:8081/actuator/metrics | jq .names

# Event Gateway
curl http://localhost:8080/actuator/metrics | jq .names
```

### View Specific Metric
```bash
# Transaction counter
curl http://localhost:8081/actuator/metrics/account.transactions.created

# Event creation counter
curl http://localhost:8080/actuator/metrics/events.created.total

# Transaction latency (with p50, p95, p99)
curl http://localhost:8081/actuator/metrics/account.transaction.duration
```

---

## Structured Logging

### Log Format (JSON)
```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "service": "event-gateway",
  "traceId": "4883b868de-ab14-bca0",
  "level": "INFO",
  "message": "Event created successfully"
}
```

### View Logs
```bash
# All logs
docker-compose logs

# Specific service
docker-compose logs event-gateway
docker-compose logs account-service

# Follow in real-time
docker-compose logs -f

# Last 50 lines
docker-compose logs --tail=50
```

---

## Docker Commands

### Build & Start
```bash
docker-compose up --build
```

### Stop Services
```bash
docker-compose down
```

### Check Status
```bash
docker-compose ps
```

### View Logs
```bash
docker-compose logs -f
```

### Restart Services
```bash
docker-compose restart
```

---

## API Examples

### Create Event
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

### Get Events
```bash
curl http://localhost:8080/events?account=ACC001
```

### Get Account Balance
```bash
curl http://localhost:8081/accounts/ACC001/balance
```

### Create Transaction
```bash
curl -X POST http://localhost:8081/accounts/ACC001/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500,
    "description": "Test transaction"
  }'
```

---

## Monitoring Dashboard

### One-Line Monitoring Loop
```bash
watch -n 5 'echo "=== Event Gateway ===" && curl -s http://localhost:8080/actuator/metrics/events.created.total | jq .measurements && echo "=== Account Service ===" && curl -s http://localhost:8081/actuator/metrics/account.transactions.created | jq .measurements'
```

### Health Check Loop
```bash
while true; do
  echo "Gateway: $(curl -s http://localhost:8080/health | jq .status) | Account: $(curl -s http://localhost:8081/health | jq .status)"
  sleep 5
done
```

---

## Common Queries

### Get Event Creation Count
```bash
curl http://localhost:8080/actuator/metrics/events.created.total | jq '.measurements[0].value'
```

### Get Error Count
```bash
curl http://localhost:8080/actuator/metrics/events.errors.total | jq '.measurements[0].value'
```

### Get Transaction Latency (p95)
```bash
curl http://localhost:8081/actuator/metrics/account.transaction.duration | jq '.measurements[] | select(.statistic=="TOTAL_TIME")'
```

### Get Active Events
```bash
curl http://localhost:8080/actuator/metrics/events.active.count | jq '.measurements[0].value'
```

---

## Troubleshooting

### Services Not Starting
```bash
docker-compose logs
docker-compose build --no-cache
docker-compose up
```

### Can't Connect to Services
```bash
docker-compose ps  # Check if running
docker-compose logs  # Check for errors
curl http://localhost:8081/health  # Test connectivity
```

### Trace IDs Not in Logs
```bash
# Verify sampling is enabled
docker-compose logs | grep traceId

# Check if spring.sleuth.sampler.probability=1.0 is set
```

### Metrics Not Showing
```bash
# Make some API calls first
curl -X POST http://localhost:8080/events -H "Content-Type: application/json" -d '{...}'

# Then check metrics
curl http://localhost:8080/actuator/metrics
```

---

## Key Ports

| Service | Port | URL |
|---------|------|-----|
| Account Service | 8081 | http://localhost:8081 |
| Event Gateway | 8080 | http://localhost:8080 |

---

## Key Endpoints

### Health
- `GET /health` - Service health check

### Metrics (Actuator)
- `GET /actuator/metrics` - List all metrics
- `GET /actuator/metrics/{metric-name}` - Get specific metric
- `GET /actuator/health` - Detailed health info
- `GET /actuator/info` - Application info

### API
- `POST /events` - Create event (Gateway)
- `GET /events` - List events (Gateway)
- `POST /accounts/{id}/transactions` - Create transaction (Account Service)
- `GET /accounts/{id}/balance` - Get balance (Account Service)

---

## Configuration Files

### Pom Files
- Root: `pom.xml`
- Account Service: `account-service/pom.xml`
- Event Gateway: `event-gateway/pom.xml`

### Properties
- Account: `account-service/src/main/resources/application.properties`
- Gateway: `event-gateway/src/main/resources/application.properties`

### Logging
- Account: `account-service/src/main/resources/logback.xml`
- Gateway: `event-gateway/src/main/resources/logback.xml`

### Docker
- Compose: `docker-compose.yml`
- Account Dockerfile: `account-service/Dockerfile`
- Gateway Dockerfile: `event-gateway/Dockerfile`

---

## Documentation

- **Full Tracing Guide**: [TRACING_IMPLEMENTATION.md](./TRACING_IMPLEMENTATION.md)
- **Docker Setup**: [DOCKER_SETUP.md](./DOCKER_SETUP.md)
- **Observability Details**: [OBSERVABILITY.md](./OBSERVABILITY.md)
- **Project Summary**: [PROJECT_SUMMARY.md](./PROJECT_SUMMARY.md)

---

## Performance Tips

### Local Development
- Use `mvn spring-boot:run` for fast iteration
- Set `spring.sleuth.sampler.probability=0.1` to reduce logging overhead
- Disable DEBUG logging for non-essential loggers

### Docker Production
- Use multi-stage build (already configured)
- Monitor with: `docker stats`
- Enable proper garbage collection: `-XX:+UseG1GC`
- Set appropriate heap size: `-Xmx256m` (or more if needed)

---

## Testing Trace Propagation

### Step 1: Create Event
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"accountId":"TEST001","eventType":"DEPOSIT","amount":100,"description":"test"}'
```

### Step 2: Extract Trace ID
```bash
TRACE_ID=$(docker-compose logs event-gateway | grep "traceId" | head -1 | grep -o '"traceId":"[^"]*' | cut -d'"' -f4)
echo "Trace ID: $TRACE_ID"
```

### Step 3: Verify Same Trace ID in Account Service Logs
```bash
docker-compose logs account-service | grep "$TRACE_ID"
```

---

## Memory & CPU Monitoring

### Docker Stats
```bash
docker stats account-service event-gateway
```

### Memory Usage
```bash
docker stats --no-stream
```

### CPU Usage
```bash
docker top account-service
docker top event-gateway
```

---

## Log Level Configuration

### Current Log Levels
- `com.eventledger` - DEBUG
- `org.springframework` - INFO
- `org.springframework.cloud.sleuth` - DEBUG

### Change Log Level (via environment variable)
```bash
export LOGGING_LEVEL_COM_EVENTLEDGER=INFO
mvn spring-boot:run
```

---

## Version Info

- Spring Boot: 2.7.14
- Spring Cloud Sleuth: 3.1.8
- OpenTelemetry: 1.27.0
- Micrometer: included with Spring Boot
- Java: 8+

---

## Quick Copy-Paste Commands

### Test Full Flow
```bash
# 1. Check health
echo "Checking health..."
curl -s http://localhost:8080/health | jq .status

# 2. Create event
echo "Creating event..."
curl -s -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"accountId":"TEST","eventType":"DEPOSIT","amount":100,"description":"test"}' | jq .

# 3. Check metrics
echo "Checking metrics..."
curl -s http://localhost:8080/actuator/metrics/events.created.total | jq .measurements
```

---

Last Updated: 2024-01-15

