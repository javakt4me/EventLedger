# Test APIs Script
Write-Host "========================================"
Write-Host "Testing EventLedger APIs"
Write-Host "========================================"
Write-Host ""

# Test 1: Health Checks
Write-Host "1. Testing Health Endpoints..."
Write-Host ""

Write-Host "Account Service Health:"
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/health" -ErrorAction SilentlyContinue
    Write-Host "✓ Status Code: $($response.StatusCode)"
    Write-Host $response.Content
    Write-Host ""
} catch {
    Write-Host "✗ Failed to connect"
    Write-Host ""
}

Write-Host "Event Gateway Health:"
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/health" -ErrorAction SilentlyContinue
    Write-Host "✓ Status Code: $($response.StatusCode)"
    Write-Host $response.Content
    Write-Host ""
} catch {
    Write-Host "✗ Failed to connect"
    Write-Host ""
}

# Test 2: GET /events (now fixed - optional parameter)
Write-Host "2. Testing GET /events (no parameters - now FIXED!)..."
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/events" -ErrorAction SilentlyContinue
    Write-Host "✓ Status Code: $($response.StatusCode)"
    Write-Host "✓ Response: $($response.Content)"
    Write-Host ""
} catch {
    Write-Host "✗ Failed: $($_.Exception.Message)"
    Write-Host ""
}

# Test 3: Create Event
Write-Host "3. Testing POST /events (Create Event)..."
try {
    $event = @{
        accountId = "ACC001"
        eventType = "DEPOSIT"
        amount = 1000
        currency = "USD"
        description = "Test deposit"
        metadata = @{
            source = "powershell"
            timestamp = Get-Date
        }
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "http://localhost:8080/events" `
        -Method POST `
        -ContentType "application/json" `
        -Body $event `
        -ErrorAction SilentlyContinue

    Write-Host "✓ Status Code: $($response.StatusCode)"
    Write-Host "✓ Event Created: $($response.Content)"
    Write-Host ""
} catch {
    Write-Host "✗ Failed: $($_.Exception.Message)"
    Write-Host ""
}

# Test 4: Get Balance
Write-Host "4. Testing GET /accounts/{id}/balance..."
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/accounts/ACC001/balance" -ErrorAction SilentlyContinue
    Write-Host "✓ Status Code: $($response.StatusCode)"
    Write-Host "✓ Balance: $($response.Content)"
    Write-Host ""
} catch {
    Write-Host "✗ Failed: $($_.Exception.Message)"
    Write-Host ""
}

Write-Host "========================================"
Write-Host "Testing Complete!"
Write-Host "========================================"

