#!/usr/bin/env pwsh
# scripts/graalvm-pgo/pgo-compare.ps1
#
# PowerShell counterpart to pgo-compare.sh.

$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot '..' '..')).Path
$PgoDir = Join-Path $ProjectDir 'target/pgo-run'

function Get-MetricField {
    param([string] $File, [string] $Key)
    if (-not (Test-Path $File)) { return '-' }
    $line = Get-Content $File | Where-Object { $_ -match "^$Key=(\d+)" } | Select-Object -First 1
    if ($line) { return $Matches[1] } else { return '-' }
}

"{0,-15} {1,12} {2,12} {3,12}" -f 'metric', 'normal', 'instrumented', 'optimized'
'----------------------------------------------------------'

foreach ($metric in @('ops_total', 'errors_total', 'ops_per_sec', 'workers_succeeded')) {
    $n = Get-MetricField (Join-Path $PgoDir 'normal.metrics') $metric
    $i = Get-MetricField (Join-Path $PgoDir 'instrumented.metrics') $metric
    $o = Get-MetricField (Join-Path $PgoDir 'optimized.metrics') $metric
    "{0,-15} {1,12} {2,12} {3,12}" -f $metric, $n, $i, $o
}
