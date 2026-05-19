#!/usr/bin/env pwsh
# scripts/graalvm-pgo/pgo-test-binary.ps1
#
# PowerShell counterpart to pgo-test-binary.sh.
#
# STUB: securedoc has no Keycloak / OIDC; workload.ps1 needs to seed an admin
# row and pluck OTPs out of the DB to drive the login flow. See workload.ps1.

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('normal', 'instrumented', 'optimized')]
    [string] $Variant
)

$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot '..' '..')).Path
$PgoDir = Join-Path $ProjectDir 'target/pgo-run'
New-Item -ItemType Directory -Force -Path $PgoDir | Out-Null

$Binary = Get-ChildItem -Path (Join-Path $ProjectDir 'target') -Filter "securedoc-*-runner-$Variant" |
          Select-Object -First 1
if (-not $Binary) {
    Write-Error "no binary found for variant=$Variant at target/securedoc-*-runner-$Variant"
    exit 1
}

Write-Host "===== pgo-test-binary[$Variant]: launching $($Binary.FullName) ====="
$logPath = Join-Path $PgoDir "$Variant.app.log"
$app = Start-Process -FilePath $Binary.FullName `
    -ArgumentList "-Xmx512m", "-Djdk.virtualThreadScheduler.parallelism=$([Environment]::ProcessorCount)" `
    -RedirectStandardOutput $logPath `
    -RedirectStandardError "$logPath.err" `
    -PassThru -NoNewWindow

try {
    Write-Host "===== pgo-test-binary[$Variant]: waiting for app ready ====="
    $ready = $false
    for ($i = 0; $i -lt 60; $i++) {
        try {
            Invoke-WebRequest -Uri 'http://localhost:8080/' -UseBasicParsing -TimeoutSec 2 | Out-Null
            $ready = $true
            break
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    if (-not $ready) {
        Write-Error "app did not become ready"
        exit 1
    }
    Write-Host "===== pgo-test-binary[$Variant]: app ready ====="

    & pwsh (Join-Path $PSScriptRoot 'parallel-workload.ps1') -Variant $Variant
    $workloadExit = $LASTEXITCODE
} finally {
    Write-Host "===== pgo-test-binary[$Variant]: shutting down app ====="
    Stop-Process -Id $app.Id -ErrorAction SilentlyContinue
    $app.WaitForExit(10000) | Out-Null

    if ($Variant -eq 'instrumented') {
        $iprof = Join-Path $ProjectDir 'default.iprof'
        if (Test-Path $iprof) {
            Move-Item -Force $iprof (Join-Path $PgoDir 'default.iprof')
            Write-Host "===== pgo-test-binary[instrumented]: default.iprof captured ====="
        } else {
            Write-Warning "instrumented run produced no default.iprof"
        }
    }
}

exit $workloadExit
