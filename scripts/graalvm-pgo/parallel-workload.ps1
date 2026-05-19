#!/usr/bin/env pwsh
# scripts/graalvm-pgo/parallel-workload.ps1
#
# PowerShell counterpart to parallel-workload.sh.

param(
    [Parameter(Mandatory = $true)] [string] $Variant
)

$Workers      = if ($env:PGO_WORKERS)      { [int]$env:PGO_WORKERS }      else { 10 }
$DurationSec  = if ($env:PGO_DURATION_SEC) { [int]$env:PGO_DURATION_SEC } else { 300 }

$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot '..' '..')).Path
$PgoDir = Join-Path $ProjectDir 'target/pgo-run'
New-Item -ItemType Directory -Force -Path $PgoDir | Out-Null

$MetricsFile = Join-Path $PgoDir "$Variant.metrics"
Write-Host "===== parallel-workload[$Variant]: $Workers workers x $DurationSec s ====="

$jobs = @()
for ($i = 1; $i -le $Workers; $i++) {
    $log = Join-Path $PgoDir "$Variant.worker-$i.log"
    $jobs += Start-Job -ScriptBlock {
        param($Script, $V, $Idx, $D, $LogPath)
        & pwsh -File $Script -Variant $V -WorkerIndex $Idx -DurationSec $D *> $LogPath
        $LASTEXITCODE
    } -ArgumentList (Join-Path $PSScriptRoot 'workload.ps1'), $Variant, $i, $DurationSec, $log
}

$succeeded = 0; $failed = 0
foreach ($job in $jobs) {
    Wait-Job $job | Out-Null
    $rc = Receive-Job $job
    if ($rc -eq 0) { $succeeded++ } else { $failed++ }
    Remove-Job $job
}

$totalOps = 0; $totalErr = 0
for ($i = 1; $i -le $Workers; $i++) {
    $log = Join-Path $PgoDir "$Variant.worker-$i.log"
    if (Test-Path $log) {
        $content = Get-Content $log -Raw
        $opsMatch = [regex]::Matches($content, 'ops=(\d+)') | Select-Object -Last 1
        $errMatch = [regex]::Matches($content, 'errors=(\d+)') | Select-Object -Last 1
        if ($opsMatch) { $totalOps += [int]$opsMatch.Groups[1].Value }
        if ($errMatch) { $totalErr += [int]$errMatch.Groups[1].Value }
    }
}

@(
    "variant=$Variant",
    "workers=$Workers",
    "duration_sec=$DurationSec",
    "workers_succeeded=$succeeded",
    "workers_failed=$failed",
    "ops_total=$totalOps",
    "errors_total=$totalErr",
    "ops_per_sec=$([math]::Floor($totalOps / [math]::Max($DurationSec,1)))"
) | Set-Content -Path $MetricsFile

Get-Content $MetricsFile

if ($succeeded -ge 1) { exit 0 } else { Write-Error 'FATAL: 0 workers succeeded'; exit 1 }
