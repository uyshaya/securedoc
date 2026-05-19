# Pre-flight check that fails the Maven build early when Docker is missing or
# the daemon is unreachable on Windows. Bound to process-test-resources via
# exec-maven-plugin so the dev sees an actionable message instead of a cryptic
# Testcontainers / pull-mysql-image error mid-build. Read-only: this script
# never installs anything.

$ErrorActionPreference = 'Stop'

# Opt out: when the dev has wired up a local MySQL via env vars, DevServices
# never starts a container, so the Docker probe is irrelevant.
if (-not [string]::IsNullOrEmpty($env:QUARKUS_DATASOURCE_JDBC_URL)) {
    exit 0
}

function Write-InstallHint {
    Write-Error -ErrorAction Continue -Message @'
Install Docker Desktop on Windows:

    winget install Docker.DockerDesktop

Docker Desktop needs WSL2 (preferred) or Hyper-V. The installer enables
the required Windows features and may prompt for a reboot. After install,
launch Docker Desktop once to finish setup.
'@

    Write-Error -ErrorAction Continue -Message @'

Alternatively, bypass Docker entirely by pointing the build at a local MySQL:

    $env:QUARKUS_DATASOURCE_JDBC_URL  = "jdbc:mysql://localhost:3306/securedoc"
    $env:QUARKUS_DATASOURCE_USERNAME = "securedoc_user"
    $env:QUARKUS_DATASOURCE_PASSWORD = "..."
'@
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error -ErrorAction Continue -Message @'
[securedoc] Docker is not installed.

This project uses Docker for:
  - Quarkus DevServices (auto-starts a mysql:8 container for dev / test)
  - The native-release-pgo profile (docker compose up)
'@
    Write-InstallHint
    exit 1
}

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Error -ErrorAction Continue -Message @'
[securedoc] Docker is installed but the daemon is not reachable.

Possible causes:
  - Docker Desktop is not running. Start it from the Start menu.
  - WSL2 or Hyper-V is disabled. Re-run the Docker Desktop installer.
  - The current user is not in the 'docker-users' group.

To bypass Docker entirely, set the QUARKUS_DATASOURCE_JDBC_URL,
QUARKUS_DATASOURCE_USERNAME, and QUARKUS_DATASOURCE_PASSWORD environment
variables.
'@
    exit 1
}
