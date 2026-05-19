#!/usr/bin/env pwsh
# scripts/graalvm-pgo/workload.ps1
#
# PowerShell counterpart to workload.sh. See workload.sh header for the full
# STUB / "needs filling in" rundown (auth via seeded admin + OTP plucked from
# MySQL, endpoint mix, etc).

param(
    [Parameter(Mandatory = $true)] [string] $Variant,
    [Parameter(Mandatory = $true)] [int]    $WorkerIndex,
    [Parameter(Mandatory = $true)] [int]    $DurationSec
)

$BaseUrl = if ($env:SECUREDOC_BASE_URL) { $env:SECUREDOC_BASE_URL } else { 'http://localhost:8080' }
$deadline = (Get-Date).AddSeconds($DurationSec)
$ops = 0
$errors = 0

Write-Host "===== workload[$Variant/w$WorkerIndex]: looping for ${DurationSec}s against $BaseUrl ====="

while ((Get-Date) -lt $deadline) {
    try {
        Invoke-WebRequest -Uri $BaseUrl -UseBasicParsing -TimeoutSec 5 | Out-Null
        $ops++
    } catch {
        $errors++
    }
}

Write-Host "ops=$ops errors=$errors"
