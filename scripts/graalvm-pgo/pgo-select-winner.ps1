#!/usr/bin/env pwsh
# scripts/graalvm-pgo/pgo-select-winner.ps1
#
# PowerShell counterpart to pgo-select-winner.sh. Optimized wins ties.

$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot '..' '..')).Path
$PgoDir = Join-Path $ProjectDir 'target/pgo-run'

function Read-Ops {
    param([string] $File)
    if (-not (Test-Path $File)) { return $null }
    $line = Get-Content $File | Where-Object { $_ -match '^ops_per_sec=(\d+)' } | Select-Object -First 1
    if ($line) { return [int]$Matches[1] } else { return $null }
}

$normal    = Read-Ops (Join-Path $PgoDir 'normal.metrics')
$optimized = Read-Ops (Join-Path $PgoDir 'optimized.metrics')

if ($null -eq $normal -or $null -eq $optimized) {
    Write-Error "FATAL: missing metrics (normal=$normal, optimized=$optimized)"
    exit 1
}

$winner = if ($optimized -ge $normal) { 'optimized' } else { 'normal' }
Write-Host "===== pgo-select-winner: $winner wins (normal=$normal, optimized=$optimized ops/sec) ====="

$winnerBin = Get-ChildItem -Path (Join-Path $ProjectDir 'target') -Filter "securedoc-*-runner-$winner" |
             Select-Object -First 1
if (-not $winnerBin) {
    Write-Error "FATAL: winner binary not found: target/securedoc-*-runner-$winner"
    exit 1
}

$canonical = $winnerBin.FullName -replace '-runner-(normal|optimized)$', '-runner'
Copy-Item -Force $winnerBin.FullName $canonical
Write-Host "===== pgo-select-winner: copied -> $canonical ====="

Get-ChildItem -Path (Join-Path $ProjectDir 'target') -Filter 'securedoc-*-runner-instrumented' |
    Remove-Item -Force -ErrorAction SilentlyContinue
