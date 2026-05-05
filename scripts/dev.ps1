param(
    [switch]$SkipInfra,
    [switch]$StopInfra,
    [switch]$Help
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$composeFile = Join-Path $root "docker\compose.yaml"
$logsDir = Join-Path $root "logs"
$apiLog = Join-Path $logsDir "api.log"
$workerLog = Join-Path $logsDir "worker.log"

if ($Help) {
    Write-Host "Usage:"
    Write-Host "  powershell -ExecutionPolicy Bypass -File scripts\dev.ps1"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -SkipInfra   Do not start MySQL and RabbitMQ"
    Write-Host "  -StopInfra   Stop MySQL and RabbitMQ when exiting"
    Write-Host "  -Help        Show this message"
    exit 0
}

New-Item -ItemType Directory -Force $logsDir | Out-Null
Remove-Item -Force $apiLog, $workerLog -ErrorAction SilentlyContinue

function Start-MavenModule {
    param(
        [string]$Name,
        [string]$Module,
        [string]$LogFile
    )

    $mvnw = Join-Path $root "mvnw.cmd"
    $command = "`"$mvnw`" -pl $Module spring-boot:run > `"$LogFile`" 2>&1"

    Write-Host "Starting $Name. Log: $LogFile"
    return Start-Process `
        -FilePath "cmd.exe" `
        -ArgumentList "/c", $command `
        -WorkingDirectory $root `
        -WindowStyle Hidden `
        -PassThru
}

if (-not $SkipInfra) {
    Write-Host "Starting infrastructure: MySQL and RabbitMQ..."
    docker compose -f $composeFile up -d --wait mysql rabbitmq
}

$previousDbUrl = $env:DB_URL
$previousDbUser = $env:DB_USER
$previousDbPassword = $env:DB_PASSWORD
$previousRabbitHost = $env:RABBITMQ_HOST
$previousRabbitPort = $env:RABBITMQ_PORT
$previousRabbitUser = $env:RABBITMQ_USER
$previousRabbitPassword = $env:RABBITMQ_PASSWORD

$api = $null
$worker = $null

try {
    $env:DB_URL = "jdbc:mysql://localhost:3307/edifiq?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
    $env:DB_USER = "root"
    $env:DB_PASSWORD = "root"
    $env:RABBITMQ_HOST = "localhost"
    $env:RABBITMQ_PORT = "5672"
    $env:RABBITMQ_USER = "guest"
    $env:RABBITMQ_PASSWORD = "guest"

    $api = Start-MavenModule -Name "api" -Module "api" -LogFile $apiLog
    $worker = Start-MavenModule -Name "worker" -Module "worker" -LogFile $workerLog

    Write-Host ""
    Write-Host "API: http://localhost:8081/api"
    Write-Host "RabbitMQ UI: http://localhost:15672"
    Write-Host "Logs:"
    Write-Host "  Get-Content -Wait logs\api.log"
    Write-Host "  Get-Content -Wait logs\worker.log"
    Write-Host ""
    Write-Host "Press Ctrl+C to stop api and worker."

    while ($true) {
        if ($api.HasExited) {
            throw "api stopped with exit code $($api.ExitCode). Check $apiLog"
        }
        if ($worker.HasExited) {
            throw "worker stopped with exit code $($worker.ExitCode). Check $workerLog"
        }
        Start-Sleep -Seconds 2
    }
}
finally {
    foreach ($process in @($api, $worker)) {
        if ($null -ne $process -and -not $process.HasExited) {
            Write-Host "Stopping process $($process.Id)..."
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        }
    }

    $env:DB_URL = $previousDbUrl
    $env:DB_USER = $previousDbUser
    $env:DB_PASSWORD = $previousDbPassword
    $env:RABBITMQ_HOST = $previousRabbitHost
    $env:RABBITMQ_PORT = $previousRabbitPort
    $env:RABBITMQ_USER = $previousRabbitUser
    $env:RABBITMQ_PASSWORD = $previousRabbitPassword

    if ($StopInfra) {
        Write-Host "Stopping infrastructure..."
        docker compose -f $composeFile stop mysql rabbitmq
    }
}
