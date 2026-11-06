
$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

$wrapper = Join-Path $root "mvnw.cmd"

if (-not (Test-Path $wrapper)) {
    Write-Host "ERROR: mvnw.cmd not found in $root" -ForegroundColor Red
    exit 1
}

$env:DB_URL = "jdbc:postgresql://localhost:5432/films_series"
$env:DB_USER = "postgres"

if (-not $env:DB_PASSWORD) {
    $secure = Read-Host "PostgreSQL password" -AsSecureString

    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)

    try {
        $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

if (-not $env:INTERNAL_SERVICE_KEY) {
    $env:INTERNAL_SERVICE_KEY = (
        [guid]::NewGuid().ToString("N") +
        [guid]::NewGuid().ToString("N")
    )
}

$services = @(
    @{ Name = "Discovery Server"; Folder = "discovery-server"; Port = 8761 },
    @{ Name = "Config Server"; Folder = "config-server"; Port = 8888 },
    @{ Name = "Catalog Service"; Folder = "catalog-service"; Port = 8082 },
    @{ Name = "User Service"; Folder = "user-service"; Port = 8084 },
    @{ Name = "Rating Service"; Folder = "rating-service"; Port = 8085 },
    @{ Name = "Admin Service"; Folder = "admin-service"; Port = 8086 },
    @{ Name = "Recommendation Service"; Folder = "recommendation-service"; Port = 8083 },
    @{ Name = "Guccispring"; Folder = "."; Port = 8081 },
    @{ Name = "API Gateway"; Folder = "api-gateway"; Port = 8080 }
)

function Test-Port {
    param([int]$Port)

    $connection = Get-NetTCPConnection `
        -LocalPort $Port `
        -State Listen `
        -ErrorAction SilentlyContinue

    return [bool]$connection
}

function Find-ServiceFolder {
    param([string]$Folder)

    $path = Join-Path $root $Folder

    if (Test-Path (Join-Path $path "pom.xml")) {
        return $path
    }

    $parent = Split-Path $root -Parent
    $path = Join-Path $parent $Folder

    if (Test-Path (Join-Path $path "pom.xml")) {
        return $path
    }

    return $null
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Yellow
Write-Host "          KADR STARTUP" -ForegroundColor Yellow
Write-Host "=====================================" -ForegroundColor Yellow
Write-Host ""

$index = 0
$failed = $false

foreach ($service in $services) {
    $index++

    $name = $service.Name
    $port = $service.Port
    $folder = $service.Folder

    Write-Host ""
    Write-Host "[$index/9] $name : $port" -ForegroundColor Cyan

    $path = Find-ServiceFolder $folder

    if (-not $path) {
        Write-Host "[ERROR] Project folder not found: $folder" -ForegroundColor Red
        $failed = $true
        break
    }

    if (Test-Port $port) {
        Write-Host "[ALREADY RUNNING] Port $port" -ForegroundColor Yellow
        continue
    }

    $pom = Join-Path $path "pom.xml"

    $command = @"
`$Host.UI.RawUI.WindowTitle = '$name'
Set-Location '$path'
& '$wrapper' -f '$pom' spring-boot:run
"@

    $encoded = [Convert]::ToBase64String(
        [Text.Encoding]::Unicode.GetBytes($command)
    )

    Start-Process `
        -FilePath "powershell.exe" `
        -WorkingDirectory $path `
        -ArgumentList @(
            "-NoExit",
            "-EncodedCommand",
            $encoded
        )

    Write-Host "[WAITING] Checking port $port..." -ForegroundColor Yellow

    $started = $false

    for ($attempt = 1; $attempt -le 60; $attempt++) {
        Start-Sleep -Seconds 2

        if (Test-Port $port) {
            $started = $true
            break
        }
    }

    if ($started) {
        Write-Host "[OK] $name is listening on port $port" -ForegroundColor Green
    }
    else {
        Write-Host "[ERROR] $name did not start" -ForegroundColor Red
        Write-Host "Check the PowerShell window for $name" -ForegroundColor Yellow

        $failed = $true
        break
    }
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Yellow

if ($failed) {
    Write-Host "STARTUP INCOMPLETE" -ForegroundColor Red
    Write-Host "Fix the failed service and run the script again."
}
else {
    Write-Host "ALL 9 PORTS ARE LISTENING" -ForegroundColor Green
    Write-Host ""
    Write-Host "Eureka: http://localhost:8761"
    Write-Host "Website: http://localhost:8080"
    Write-Host "Main app: http://localhost:8081"
}

Write-Host "=====================================" -ForegroundColor Yellow
