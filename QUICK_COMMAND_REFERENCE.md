# Quick Command Reference

## 🎯 Most Common Commands

### Generate JaCoCo Coverage Report (Main Task)
```powershell
cd D:\workspace\EventLedger
mvn clean test jacoco:report
```
**Output**: Coverage reports in `target/site/jacoco/index.html`

### Run All Tests (Quick)
```powershell
mvn test
```

### View Coverage Reports
```powershell
start event-gateway\target\site\jacoco\index.html
start account-service\target\site\jacoco\index.html
```

---

## 📊 Test Execution Commands

### Run Specific Tests
```powershell
# Edge case tests only
mvn test -Dtest=EventGatewayControllerEdgeCasesTest

# All edge case tests
mvn test -Dtest=*EdgeCases*

# Integration tests only
mvn test -Dtest=*Integration*

# Config tests
mvn test -Dtest=*Config*

# Circuit breaker test
mvn test -Dtest=EventGatewayCircuitBreakerIntegrationTest

# Single module
cd event-gateway
mvn test

cd account-service
mvn test
```

### Run with Debugging
```powershell
# Verbose output
mvn test -X

# Show test output
mvn test -Dtest=EventGatewayServiceTest -X

# Fail on first error
mvn test -ff
```

---

## 🐳 Docker Commands

### Build Services
```powershell
docker-compose build
```

### Start Services
```powershell
docker-compose up -d
```

### Stop Services
```powershell
docker-compose down
```

### View Logs
```powershell
docker-compose logs -f
docker-compose logs event-gateway
docker-compose logs account-service
```

### Run E2E Tests
```powershell
pwsh -ExecutionPolicy Bypass -File .\scripts\e2e-docker-test.ps1
```

---

## 📈 Coverage Reports

### Generate JaCoCo Report
```powershell
mvn jacoco:report
```

### Generate with Clean
```powershell
mvn clean test jacoco:report
```

### Run Custom Script
```powershell
pwsh -ExecutionPolicy Bypass -File .\scripts\run-jacoco-report.ps1
```

### View Report Location
```powershell
event-gateway\target\site\jacoco\index.html
account-service\target\site\jacoco\index.html
```

---

## 🏗️ Build Commands

### Clean Build
```powershell
mvn clean install
```

### Build Only
```powershell
mvn clean package -DskipTests
```

### Build Both Modules
```powershell
# From root
mvn clean install

# Or specific order
cd account-service && mvn clean install
cd event-gateway && mvn clean install
```

### Verify Only
```powershell
mvn clean verify
```

---

## 🔍 Dependency Management

### Check Dependencies
```powershell
mvn dependency:tree
mvn dependency:resolve
```

### Download Dependencies
```powershell
mvn dependency:go-offline
```

### Check for Updates
```powershell
mvn versions:display-dependency-updates
```

---

## 📝 Documentation Commands

### View README
```powershell
cat README.md

# Or with PowerShell
Get-Content README.md

# In browser
start README.md
```

### Check All Docs
```powershell
ls *.md

# Count markdown files
(Get-ChildItem *.md).Count
```

### View Build Guide
```powershell
start BUILD.md
```

### View Coverage Guide
```powershell
start TEST_COVERAGE.md
```

---

## 🚀 Complete Workflow

### 1. Build and Test
```powershell
cd D:\workspace\EventLedger
mvn clean test
```

### 2. Generate Coverage Report
```powershell
mvn jacoco:report
```

### 3. View Report
```powershell
start event-gateway\target\site\jacoco\index.html
```

### 4. Run Docker E2E
```powershell
docker-compose build
docker-compose up -d

pwsh -ExecutionPolicy Bypass -File .\scripts\e2e-docker-test.ps1

docker-compose down
```

### 5. Commit and Push
```powershell
git add .
git commit -m "Add test coverage improvements: 25+ new tests, target 90% coverage"
git push origin main
```

---

## ⚡ Performance Optimization

### Run Tests in Parallel
```powershell
mvn test -T 1C
```

### Skip Integration Tests (Fast)
```powershell
mvn test -DskipITs
```

### Set Maven Memory
```powershell
$env:MAVEN_OPTS = "-Xmx1024m -Xms512m"
mvn clean test
```

---

## 🔧 Troubleshooting Commands

### Check Maven Version
```powershell
mvn --version
```

### Check Java Version
```powershell
java -version
```

### Clear Maven Cache
```powershell
rm -r ~/.m2/repository
```

### Check Port Usage
```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :8081
```

### Kill Process on Port
```powershell
$p = Get-NetTCPConnection -LocalPort 8080 | Select-Object -ExpandProperty OwningProcess
Stop-Process -Id $p -Force
```

---

## 📊 Test Report Analysis

### Find Test Summary
```powershell
cat target/surefire-reports/TEST-*.xml | Select-String "tests="
```

### Check for Failures
```powershell
ls target/surefire-reports/*.txt | Where-Object { Select-String "FAILURE" $_ }
```

### View Coverage Summary
```powershell
# Opens HTML report
start event-gateway\target\site\jacoco\index.html
```

---

## 🔄 CI/CD Commands

### Simulate GitHub Actions Locally
```powershell
# Run what CI runs
mvn clean test
mvn jacoco:report
docker-compose build
```

### Push Trigger CI
```powershell
git push origin main
# Or: git push origin <branch-name>
```

### View CI Results
```powershell
# Go to https://github.com/your-repo/actions
```

---

## 📚 Documentation Navigation

| Command | Purpose |
|---------|---------|
| `cat BUILD.md` | Build guide |
| `cat TEST_COVERAGE.md` | Coverage metrics |
| `cat IMPLEMENTATION_INDEX.md` | Quick index |
| `cat JACOCO_TEST_REPORT.md` | Coverage help |
| `cat COMPLETION_CHECKLIST.md` | Verification |
| `start README.md` | Project overview |

---

## ✅ Verification Checklist Commands

```powershell
# 1. Verify compilation
mvn clean compile
echo "If no errors, compilation OK"

# 2. Verify tests
mvn test
echo "If BUILD SUCCESS, tests passed"

# 3. Verify coverage
mvn jacoco:report
echo "If report generated, coverage OK"

# 4. Verify reports exist
Test-Path event-gateway\target\site\jacoco\index.html
Test-Path account-service\target\site\jacoco\index.html

# 5. Verify Docker
docker-compose build
echo "If no errors, Docker OK"
```

---

## 🎯 Target Commands by Use Case

### I want to just run tests
```powershell
mvn test
```

### I want to check coverage
```powershell
mvn clean test jacoco:report
start event-gateway\target\site\jacoco\index.html
```

### I want to test Docker deployment
```powershell
docker-compose build
docker-compose up -d
pwsh -ExecutionPolicy Bypass -File .\scripts\e2e-docker-test.ps1
docker-compose down
```

### I want to verify everything
```powershell
mvn clean test
mvn jacoco:report
docker-compose build
docker-compose up -d
pwsh -ExecutionPolicy Bypass -File .\scripts\e2e-docker-test.ps1
docker-compose down
```

### I want to generate all reports
```powershell
mvn clean test jacoco:report
```

### I want to run one test file
```powershell
mvn test -Dtest=EventGatewayServiceTest
```

### I want to push to GitHub
```powershell
git add .
git commit -m "Test coverage improvements"
git push origin main
```

---

## 📞 Getting Help

### Help with Maven
```powershell
mvn --help
mvn help:describe -Dplugin=maven-compiler-plugin
```

### Help with Tests
```powershell
mvn help:describe -Dplugin=maven-surefire-plugin
```

### Help with Coverage
```powershell
mvn help:describe -Dplugin=jacoco-maven-plugin
```

### Help with Specific Plugin
```powershell
mvn <plugin>:help
# Example:
mvn surefire:help
```

---

## 🎉 Summary

**Main Command**: `mvn clean test jacoco:report`

**Expected Result**: 
- ✅ 100+ tests passing
- ✅ ~90% coverage achieved
- ✅ Reports generated

**Next Step**: View reports at `target/site/jacoco/index.html`

---

**Last Updated**: July 23, 2026  
**Status**: Ready to use ✅
