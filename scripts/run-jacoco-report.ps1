# Run JaCoCo test coverage report
# Usage: From repository root run: powershell -ExecutionPolicy Bypass -File .\scripts\run-jacoco-report.ps1

Write-Host "EventLedger JaCoCo Test Coverage Report"
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Date: $(Get-Date)" -ForegroundColor Gray
Write-Host ""

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
cd $root

# Color functions
function Write-Success($msg) {
    Write-Host "✓ $msg" -ForegroundColor Green
}

function Write-Error($msg) {
    Write-Host "✗ $msg" -ForegroundColor Red
}

function Write-Info($msg) {
    Write-Host "ℹ $msg" -ForegroundColor Cyan
}

# Check if Maven is available
try {
    $mvnVersion = mvn --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Maven not found"
    }
    Write-Success "Maven is available"
} catch {
    Write-Error "Maven is not available in PATH"
    Write-Info "Please install Maven and add it to PATH, or use Docker:"
    Write-Host "  docker run --rm -v $(pwd):/workspace -w /workspace maven:3.8-jdk-8 mvn clean test jacoco:report"
    exit 1
}

Write-Info "Building and running tests with JaCoCo coverage..."
Write-Host ""

# Run tests with coverage
mvn clean test jacoco:report

if ($LASTEXITCODE -ne 0) {
    Write-Error "Tests failed or coverage report generation failed"
    exit 1
}

Write-Success "Tests completed"
Write-Host ""

# Parse and display coverage results
Write-Info "Coverage Reports Generated:"
Write-Host ""

$reportPaths = @(
    "account-service/target/site/jacoco/index.html",
    "event-gateway/target/site/jacoco/index.html"
)

foreach ($path in $reportPaths) {
    if (Test-Path $path) {
        Write-Success "Report: $path"
    }
}

Write-Host ""
Write-Info "To view coverage reports:"
Write-Host "  - Account Service:  file://$(pwd)\account-service\target\site\jacoco\index.html"
Write-Host "  - Event Gateway:    file://$(pwd)\event-gateway\target\site\jacoco\index.html"

# Display summary statistics
Write-Host ""
Write-Info "Test Summary:"

$accountTestLog = "account-service/target/surefire-reports"
$gatewayTestLog = "event-gateway/target/surefire-reports"

if (Test-Path $accountTestLog) {
    $accountTests = Get-ChildItem -Path $accountTestLog -Filter "*.xml" | Measure-Object | Select-Object -ExpandProperty Count
    Write-Host "  Account Service Tests: $accountTests test files"
}

if (Test-Path $gatewayTestLog) {
    $gatewayTests = Get-ChildItem -Path $gatewayTestLog -Filter "*.xml" | Measure-Object | Select-Object -ExpandProperty Count
    Write-Host "  Event Gateway Tests:   $gatewayTests test files"
}

Write-Host ""
Write-Success "JaCoCo Coverage Report Complete"
exit 0
