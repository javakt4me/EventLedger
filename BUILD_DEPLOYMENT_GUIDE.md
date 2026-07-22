# Build & Deployment Guide

## Prerequisites

### System Requirements
- Operating System: Windows, macOS, or Linux
- Java: 8+ (tested with Java 8)
- Maven: 3.6+
- Docker: 20.10+ (for Docker deployment)
- Docker Compose: 1.29+ (for orchestrated deployment)

### Installation

**Windows:**
- Download Java 8 from Oracle
- Download Maven from apache.org
- Download Docker Desktop

**macOS:**
```bash
brew install openjdk@8
brew install maven
brew install docker
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install openjdk-8-jdk maven docker.io docker-compose
```

---

## Building the Project

### Step 1: Clone/Navigate to Project
```bash
cd EventLedger
```

### Step 2: Build All Modules
```bash
# Build entire project
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Build specific module
mvn clean install -f account-service/pom.xml
```

### Step 3: Verify Build

If build succeeds, you should see:
```
[INFO] EventLedger System .................................... SUCCESS
[INFO] Account Service ....................................... SUCCESS
[INFO] Event Gateway .......................................... SUCCESS
[INFO] BUILD SUCCESS
```

### Step 4: Build Output

JAR files are created in:
- `account-service/target/account-service-1.0.0.jar`
- `event-gateway/target/event-gateway-1.0.0.jar`

---

## Running Locally (Non-Docker)

### Option 1: Run from Command Line

**Terminal 1: Account Service**
```bash
cd account-service
mvn spring-boot:run
```

**Terminal 2: Event Gateway**
```bash
cd event-gateway
mvn spring-boot:run
```

Expected output:
```
Tomcat started on port(s): 8081 (http) with context path ''
Account Service started successfully
```

### Option 2: Run JAR Files

```bash
# Build first
mvn clean package -DskipTests

# Terminal 1: Account Service
java -jar account-service/target/account-service-1.0.0.jar

# Terminal 2: Event Gateway
java -jar event-gateway/target/event-gateway-1.0.0.jar
```

### Option 3: Run from IDE

1. Open project in IntelliJ IDEA or Eclipse
2. Right-click on `AccountServiceApplication.java`
3. Select "Run 'AccountServiceApplication'"
4. Repeat for `EventGatewayApplication.java`

---

## Docker Deployment

### Build Docker Images

```bash
# Navigate to project root
cd EventLedger

# Build using docker-compose
docker-compose build

# Or build individual images
docker build -t eventledger/account-service -f account-service/Dockerfile .
docker build -t eventledger/event-gateway -f event-gateway/Dockerfile .
```

### Start Services with Docker Compose

```bash
# Start all services
docker-compose up

# Start in background
docker-compose up -d

# Start with fresh build
docker-compose up --build

# Rebuild without cache
docker-compose build --no-cache
docker-compose up
```

### Check Status

```bash
# View running services
docker-compose ps

# View logs
docker-compose logs

# View logs for specific service
docker-compose logs account-service
docker-compose logs event-gateway

# Follow logs in real-time
docker-compose logs -f
```

### Stop Services

```bash
# Stop services
docker-compose stop

# Stop and remove containers
docker-compose down

# Remove everything including volumes
docker-compose down -v
```

---

## Troubleshooting Build Issues

### Issue: Maven Not Found
```bash
# Check Maven installation
mvn -version

# Add Maven to PATH if needed
export PATH=$PATH:/path/to/maven/bin
```

### Issue: Java Not Found
```bash
# Check Java installation
java -version

# Set JAVA_HOME
export JAVA_HOME=/path/to/java
export PATH=$PATH:$JAVA_HOME/bin
```

### Issue: Dependency Download Failures
```bash
# Clear Maven cache
mvn clean

# Try with offline mode disabled
mvn -U clean install

# Check proxy settings in ~/.m2/settings.xml
```

### Issue: Build Hangs
```bash
# Increase Maven timeout
mvn clean install -Dorg.slf4j.simpleLogger.defaultLogLevel=debug

# Use parallel builds
mvn -T 1C clean install
```

### Issue: Docker Build Fails
```bash
# Check Docker is running
docker ps

# Rebuild without cache
docker-compose build --no-cache

# Check Docker logs
docker-compose logs

# Verify Docker has enough disk space
docker system df
```

---

## Verification Steps

### Verify Local Build

After building locally, verify:

```bash
# 1. Check services are running
curl http://localhost:8081/health
curl http://localhost:8080/health

# 2. Check metrics
curl http://localhost:8081/actuator/metrics
curl http://localhost:8080/actuator/metrics

# 3. Create an event
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"accountId":"TEST","eventType":"DEPOSIT","amount":100,"description":"test"}'

# 4. Check logs for trace ID
# Look for traceId in console output
```

### Verify Docker Build

After Docker build, verify:

```bash
# 1. Check images
docker images | grep eventledger

# 2. Start services
docker-compose up -d

# 3. Check containers running
docker-compose ps

# 4. Test health endpoints
curl http://localhost:8081/health
curl http://localhost:8080/health

# 5. Test API
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"accountId":"TEST","eventType":"DEPOSIT","amount":100,"description":"test"}'

# 6. Check logs
docker-compose logs | grep traceId
```

---

## Production Deployment

### Docker Registry Push

```bash
# Login to registry
docker login registry.example.com

# Tag images
docker tag eventledger/account-service:latest registry.example.com/account-service:1.0.0
docker tag eventledger/event-gateway:latest registry.example.com/event-gateway:1.0.0

# Push to registry
docker push registry.example.com/account-service:1.0.0
docker push registry.example.com/event-gateway:1.0.0
```

### Kubernetes Deployment

```yaml
# Example: account-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: account-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: account-service
  template:
    metadata:
      labels:
        app: account-service
    spec:
      containers:
      - name: account-service
        image: registry.example.com/account-service:1.0.0
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_SLEUTH_SAMPLER_PROBABILITY
          value: "1.0"
        livenessProbe:
          httpGet:
            path: /health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
```

### Environment Configuration

**Production Environment Variables:**

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-host:5432/accountdb
SPRING_DATASOURCE_USERNAME=dbuser
SPRING_DATASOURCE_PASSWORD=dbpassword

# Tracing
SPRING_SLEUTH_SAMPLER_PROBABILITY=0.1  # Reduce sampling in production
SPRING_SLEUTH_TRACEID128=true

# Logging
LOGGING_LEVEL_COM_EVENTLEDGER=INFO  # Reduce from DEBUG

# Metrics
MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED=true
```

---

## Performance Tuning

### JVM Tuning

Update Dockerfile for production:

```dockerfile
# For higher load
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "app.jar"]

# For lower latency
ENTRYPOINT ["java", "-Xmx1g", "-Xms512m", "-XX:+UseZGC", "-jar", "app.jar"]
```

### Database Connection Pool

Add to application.properties:

```properties
# Connection pool size
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# Timeout settings
spring.datasource.hikari.connection-timeout=10000
spring.datasource.hikari.idle-timeout=600000
```

### Metrics Sampling

```properties
# Reduce metric cardinality in production
spring.sleuth.sampler.probability=0.1

# Batch metrics export
management.metrics.export.prometheus.step=1m
```

---

## Monitoring Build Pipeline

### CI/CD Integration (GitHub Actions)

```yaml
name: Build and Test

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up Java
      uses: actions/setup-java@v2
      with:
        java-version: '8'
    
    - name: Build with Maven
      run: mvn clean install -DskipTests
    
    - name: Build Docker images
      run: docker-compose build
    
    - name: Run tests
      run: mvn test
    
    - name: Push to registry
      if: github.ref == 'refs/heads/main'
      run: |
        docker login -u ${{ secrets.DOCKER_USERNAME }} -p ${{ secrets.DOCKER_PASSWORD }}
        docker-compose push
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean install -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        
        stage('Docker Build') {
            steps {
                sh 'docker-compose build'
            }
        }
        
        stage('Docker Push') {
            when {
                branch 'main'
            }
            steps {
                sh 'docker-compose push'
            }
        }
    }
}
```

---

## Rollback Procedure

### Docker Rollback

```bash
# Keep previous version tag
docker tag eventledger/event-gateway:latest eventledger/event-gateway:v1.0.0-stable

# Rollback to previous version
docker-compose down
docker pull eventledger/event-gateway:v1.0.0-stable
docker tag eventledger/event-gateway:v1.0.0-stable eventledger/event-gateway:latest
docker-compose up
```

### Database Rollback

```bash
# If using migration tool (Flyway/Liquibase)
mvn flyway:undo

# Manual rollback
# Connect to database and revert changes
```

---

## Health Check Procedures

### Post-Deployment Verification

```bash
#!/bin/bash

echo "=== Health Checks ==="

# Check Account Service
echo "Account Service..."
curl -f http://localhost:8081/health || echo "FAILED"

# Check Event Gateway
echo "Event Gateway..."
curl -f http://localhost:8080/health || echo "FAILED"

# Check Metrics
echo "Metrics..."
curl -f http://localhost:8081/actuator/metrics || echo "FAILED"

# Test API Flow
echo "API Flow..."
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"accountId":"HEALTH_CHECK","eventType":"TEST","amount":100,"description":"Health check"}' || echo "FAILED"

echo "=== All Checks Complete ==="
```

---

## Maintenance Tasks

### Regular Maintenance

```bash
# Weekly: Check logs for errors
docker-compose logs | grep -i error

# Weekly: Verify metrics are collecting
curl http://localhost:8080/actuator/metrics | jq '.names | length'

# Monthly: Review and clean old logs
docker exec event-gateway sh -c "find /app -name '*.log' -mtime +30 -delete"

# Monthly: Update dependencies
mvn versions:update-properties
mvn clean install -DskipTests
```

### Backup Procedures

```bash
# Backup database
docker exec eventledger-db pg_dump -U postgres > backup.sql

# Backup configuration
cp docker-compose.yml docker-compose.yml.backup
cp -r account-service/src account-service/src.backup
cp -r event-gateway/src event-gateway/src.backup
```

---

## Common Commands Reference

| Task | Command |
|------|---------|
| Build locally | `mvn clean install` |
| Run locally | `mvn spring-boot:run` |
| Build Docker | `docker-compose build` |
| Start Docker | `docker-compose up -d` |
| Stop Docker | `docker-compose down` |
| View logs | `docker-compose logs -f` |
| Health check | `curl http://localhost:8081/health` |
| Test API | `curl -X POST http://localhost:8080/events -H "Content-Type: application/json" -d {...}` |
| View metrics | `curl http://localhost:8080/actuator/metrics` |

---

## Useful Links

- [Maven Documentation](https://maven.apache.org/)
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Sleuth](https://spring.io/projects/spring-cloud-sleuth)

---

## Support

For build and deployment issues:

1. Check the troubleshooting section above
2. Review the logs: `docker-compose logs`
3. Verify prerequisites are installed
4. Check the documentation files:
   - `DOCKER_SETUP.md` - Docker guide
   - `TRACING_IMPLEMENTATION.md` - Tracing guide
   - `QUICK_REFERENCE.md` - Quick commands

---

Last Updated: January 15, 2024

