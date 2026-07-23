# Build & Test Coverage Guide

## Overview

This guide covers building the EventLedger system, running tests, and analyzing code coverage using JaCoCo.

## Quick Start

### Prerequisites

- **Java 8+** (tested with Java 8, 11, 17)
- **Maven 3.6+**
- **Docker** (optional, for containerized builds)

### Build & Test with Coverage

#### Option 1: Local Maven Build

```powershell
# From repository root
cd D:\workspace\EventLedger

# Build and run all tests with JaCoCo coverage report
mvn clean test jacoco:report

# View coverage reports
start "file://$(pwd)\event-gateway\target\site\jacoco\index.html"
start "file://$(pwd)\account-service\target\site\jacoco\index.html"
```

#### Option 2: Using PowerShell Script

```powershell
cd D:\workspace\EventLedger
pwsh -ExecutionPolicy Bypass -File .\scripts\run-jacoco-report.ps1
```

#### Option 3: Docker Build

```powershell
cd D:\workspace\EventLedger
docker-compose build
docker-compose run --rm account-service mvn clean test jacoco:report
docker-compose run --rm event-gateway mvn clean test jacoco:report
```

## Test Coverage Analysis

### Current Baseline (Before Improvements)

| Module           | Instruction Coverage | Branch Coverage | Status       |
|------------------|----------------------|-----------------|--------------|
| account-service  | 75%                  | 55%             | Medium       |
| event-gateway    | 77%                  | 76%             | Good         |
| **Combined**     | **~76%**             | —               | Medium       |

### Areas with Lower Coverage

1. **Config Packages (12-58%)**
   - RestTemplateConfig
   - Resilience4j Configuration
   - Spring Boot Auto-configuration
   
2. **Controller Packages (40-53%)**
   - HealthController edge cases
   - EventGatewayController error handling
   - Exception scenarios

3. **Exception Handling (Not Fully Covered)**
   - ServiceUnavailableException fallback paths
   - Database connection failures
   - Null/empty responses from downstream

### Coverage Improvements Made

#### New Test Files Added

1. **RestTemplateConfigTest.java**
   - Tests RestTemplate bean creation
   - Validates timeout configuration
   - Verifies ObjectMapper configuration
   - Checks date serialization settings

2. **EventGatewayControllerEdgeCasesTest.java**
   - Tests all HTTP status codes (201, 200, 400, 503, 500)
   - Covers idempotency behavior (created flag)
   - Tests validation errors
   - Tests service unavailability
   - Tests internal errors

3. **HealthControllerEdgeCasesTest.java**
   - Tests database health check (UP/DOWN)
   - Tests downstream service health check
   - Tests error scenarios (connection refused, null responses)
   - Tests service unavailability

4. **EventGatewayCircuitBreakerIntegrationTest.java** (Previously added)
   - Tests circuit breaker state transitions
   - Verifies OPEN → HALF-OPEN → CLOSED flow
   - Confirms failure and recovery handling

5. **ResilienceIntegrationTest.java** (Previously added)
   - Tests retry behavior (3 attempts)
   - Tests success after retries
   - Tests always-fail scenarios

6. **EventGatewayTraceIntegrationTest.java** (Previously added)
   - Tests trace header propagation
   - Tests idempotency (201 vs 200)
   - Tests W3C and B3 trace headers

### Expected Coverage After Improvements

| Module           | Expected Instruction Coverage | Target |
|------------------|-------------------------------|--------|
| account-service  | 85-90%                        | High   |
| event-gateway    | 90-95%                        | Very High |
| **Combined**     | **~90%**                      | Target |

*Note: 100% coverage for utility code and generated classes is impractical. Our target is high coverage for critical paths.*

## Running Test Suites

### Run All Tests

```powershell
mvn clean test
```

### Run Unit Tests Only (Exclude Integration Tests)

```powershell
mvn test -DexcludedGroups=integration
```

### Run Specific Module Tests

```powershell
# Account Service
cd account-service
mvn test

# Event Gateway
cd event-gateway
mvn test
```

### Run Specific Test Class

```powershell
mvn test -Dtest=EventGatewayServiceTest
mvn test -Dtest=HealthControllerEdgeCasesTest
```

### Run with Specific Coverage Profile

```powershell
mvn clean test jacoco:report -Pjacoco
```

## Interpreting JaCoCo Reports

### HTML Report Structure

```
target/site/jacoco/index.html
├── Summary (total coverage %)
├── Packages (breakdown by package)
├── Classes (individual class coverage)
└── Source Files (line-by-line coverage)
```

### Coverage Indicators

- **Green**: Line/branch is covered
- **Yellow**: Line/branch is partially covered
- **Red**: Line/branch is not covered

### Understanding Metrics

1. **Line Coverage**: Percentage of executable lines executed
2. **Branch Coverage**: Percentage of code branches (if/else, loops) executed
3. **Complexity**: Cyclomatic complexity (how many different paths through code)

## Best Practices

### Coverage Goals

- **Critical Paths**: 90%+ (business logic, validation, error handling)
- **Configuration**: 80%+ (Spring config, utilities)
- **Controllers**: 85%+ (all status codes, exceptions)
- **Unrealistic 100% Target**: Avoid pursuing coverage for:
  - Generated code (Lombok, JPA)
  - Third-party integrations
  - Defensive null-checks after validation

### Test Organization

```
src/test/java/
├── com/eventledger/eventgateway/
│   ├── service/           (business logic tests)
│   ├── controller/        (API endpoint tests)
│   ├── config/            (configuration tests)
│   ├── integration/       (integration tests with WireMock)
│   └── ...
```

### Test Naming Conventions

- **Unit Tests**: `ServiceNameTest.java`
- **Edge Case Tests**: `ServiceNameEdgeCasesTest.java`
- **Integration Tests**: `ServiceNameIntegrationTest.java`
- **Test Methods**: `testScenarioAndExpectation()` (e.g., `testCreateEventIdempotency()`)

## Continuous Integration

### GitHub Actions Workflow

The `.github/workflows/ci.yml` file runs:

1. Build with JDK 11
2. Run all tests
3. Generate JaCoCo report
4. Run Docker Compose e2e tests

Push to trigger CI:

```powershell
git push origin main
```

## Troubleshooting

### Maven Not Found

```powershell
# Install Maven
choco install maven

# Or add to PATH manually
$env:Path += ";C:\path\to\maven\bin"
```

### Tests Failing

1. Check test logs: `target/surefire-reports/`
2. Verify database/service connectivity for integration tests
3. Ensure Docker containers are running (if using docker-compose)

### JaCoCo Report Not Generated

```powershell
# Verify plugin is configured in pom.xml
mvn help:describe -Dplugin=org.jacoco:jacoco-maven-plugin

# Force regenerate
mvn clean test jacoco:report -DskipTests=false -Djacoco.skip=false
```

### Port Conflicts (Docker)

```powershell
# Check ports in use
netstat -ano | findstr :8080

# Kill process using port
taskkill /PID <PID> /F

# Or use different ports in docker-compose.yml
```

## Performance Optimization

### Speed Up Tests

```powershell
# Run tests in parallel
mvn test -T 1C

# Skip integration tests (for quick feedback)
mvn test -DskipITs=true

# Offline mode (if dependencies already cached)
mvn test -o
```

### Memory Configuration

```powershell
# Set Maven memory
$env:MAVEN_OPTS = "-Xmx1024m -Xms512m"
mvn clean test
```

## Documentation Links

- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [WireMock Documentation](https://wiremock.org/)
- [Resilience4j Testing](https://resilience4j.readme.io/)

## Next Steps

1. Run `mvn clean test jacoco:report` to generate coverage reports
2. Review coverage gaps in `target/site/jacoco/index.html`
3. Add tests for uncovered lines (target >= 90% on critical paths)
4. Push changes to trigger CI validation
5. Monitor coverage trends over time (add plugins like Sonar, Codacy, Codecov)
