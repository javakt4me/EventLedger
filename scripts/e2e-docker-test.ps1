# E2E Docker Compose test script
# Usage: From repository root run: powershell -ExecutionPolicy Bypass -File .\scripts\e2e-docker-test.ps1

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
cd $root

Write-Host "Bringing up docker-compose..."
docker-compose up --build -d

# Wait for services to become healthy
$gatewayUrl = 'http://localhost:8080'
$accountUrl = 'http://localhost:8081'
$maxRetries = 30
$wait = 2

function Wait-ForUrl($url, $path) {
    param($url, $path)
    $full = "$url$path"
    for ($i = 0; $i -lt $maxRetries; $i++) {
        try {
            $r = Invoke-RestMethod -Method Get -Uri $full -ErrorAction Stop
            Write-Host "$full is responsive"
            return $true
        } catch {
            Write-Host "Waiting for $full... ($i)"
            Start-Sleep -Seconds $wait
        }
    }
    return $false
}

if (-not (Wait-ForUrl $gatewayUrl '/events/health')) {
    Write-Host "Gateway health endpoint did not become ready in time" -ForegroundColor Red
    docker-compose logs
    docker-compose down
    exit 1
}

if (-not (Wait-ForUrl $accountUrl '/actuator/health')) {
    Write-Host "Account service health endpoint did not become ready in time" -ForegroundColor Red
    docker-compose logs
    docker-compose down
    exit 1
}

# Submit a test event
$event = @{
    eventId = "evt-docker-e2e-1"
    accountId = "acct-docker-1"
    type = "CREDIT"
    amount = 123.45
    currency = "USD"
    eventTimestamp = (Get-Date).ToUniversalTime().ToString("s") + "Z"
    metadata = @{ source = "e2e-test" }
} | ConvertTo-Json -Depth 5

function Post-Event($json) {
    try {
        $r = Invoke-RestMethod -Method Post -Uri "$gatewayUrl/events" -Body $json -ContentType 'application/json' -ErrorAction Stop
        return @{ success = $true; body = $r }
    } catch {
        return @{ success = $false; error = $_ }
    }
}

# First submission - expect created
$first = Post-Event $event
if (-not $first.success) {
    Write-Host "First POST /events failed: $($first.error)" -ForegroundColor Red
    docker-compose logs
    docker-compose down
    exit 1
}

Write-Host "First POST response:"; $first.body | ConvertTo-Json

# Second submission (duplicate) - expect 200 OK and created=false
$second = Post-Event $event
if (-not $second.success) {
    Write-Host "Second POST /events failed: $($second.error)" -ForegroundColor Red
    docker-compose logs
    docker-compose down
    exit 1
}

Write-Host "Second POST response:"; $second.body | ConvertTo-Json

if ($second.body.created -ne $false) {
    Write-Host "Idempotency check failed: expected created=false on duplicate submission" -ForegroundColor Red
    docker-compose logs
    docker-compose down
    exit 1
}

Write-Host "E2E test completed successfully. Tearing down containers..."
docker-compose down
exit 0
