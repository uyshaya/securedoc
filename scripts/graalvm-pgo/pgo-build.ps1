#!/usr/bin/env pwsh
# scripts/graalvm-pgo/pgo-build.ps1
#
# PowerShell counterpart to pgo-build.sh. Canonical entry point for the
# native-release-pgo Maven build on Windows. Wraps `mvnw -P native-release-pgo`
# with a finally block + Ctrl-C handler that brings the docker compose stack
# down whether the build succeeds, fails, or is interrupted.
#
# Usage:
#   pwsh scripts/graalvm-pgo/pgo-build.ps1 install
#   pwsh scripts/graalvm-pgo/pgo-build.ps1 verify -DskipTests
#
# STUB: the `native-release-pgo` Maven profile is not yet defined in pom.xml.
# See README.md in this directory for the full pipeline design.

$ProjectDir  = (Resolve-Path (Join-Path $PSScriptRoot '..' '..')).Path
$ComposeFile = Join-Path $ProjectDir 'scripts/graalvm-pgo/docker-compose.yml'

# Ctrl-C handler: PowerShell raises a PipelineStoppedException that's caught
# by the try/finally below; this just makes Ctrl-C feel responsive.
[Console]::TreatControlCAsInput = $false

$mvnExit = 1
try {
    Set-Location $ProjectDir
    if ($IsWindows) { & .\mvnw.cmd -B -P native-release-pgo @args }
    else            { & ./mvnw    -B -P native-release-pgo @args }
    $mvnExit = $LASTEXITCODE
}
finally {
    Write-Host ''
    Write-Host "===== pgo-build: tearing down docker compose stack (rc=$mvnExit) ====="
    docker compose -f $ComposeFile down -v 2>&1 | ForEach-Object { Write-Host $_ }
}

exit $mvnExit
