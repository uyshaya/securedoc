#!/usr/bin/env bash
# Pre-flight check that fails the Maven build early when Docker is missing or
# the daemon is unreachable. Bound to process-test-resources via exec-maven-plugin
# so the dev sees an actionable message instead of a cryptic Testcontainers /
# pull-mysql-image error mid-build. Read-only: this script never installs
# anything.

set -euo pipefail

# Opt out: when the dev has wired up a local MySQL via env vars, DevServices
# never starts a container, so the Docker probe is irrelevant.
if [[ -n "${QUARKUS_DATASOURCE_JDBC_URL:-}" ]]; then
    exit 0
fi

print_install_hint() {
    local kernel
    kernel="$(uname -s)"

    case "$kernel" in
        Darwin)
            cat <<'EOF'
Install Docker Desktop on macOS:

    brew install --cask docker

Then launch Docker.app once to finish setup (it installs a privileged helper).
EOF
            ;;
        Linux)
            local distro_id=""
            if [[ -r /etc/os-release ]]; then
                distro_id="$(. /etc/os-release && echo "${ID:-}${ID_LIKE:+ $ID_LIKE}")"
            fi

            case "$distro_id" in
                *debian*|*ubuntu*)
                    cat <<'EOF'
Install Docker on Debian / Ubuntu:

    sudo apt-get update
    sudo apt-get install -y docker.io
    sudo usermod -aG docker "$USER"

Log out and back in so the docker group membership takes effect.
EOF
                    ;;
                *fedora*|*rhel*|*centos*)
                    cat <<'EOF'
Install Docker on Fedora / RHEL / CentOS:

    sudo dnf install -y docker-ce docker-ce-cli containerd.io
    sudo systemctl enable --now docker
    sudo usermod -aG docker "$USER"

Log out and back in so the docker group membership takes effect.
EOF
                    ;;
                *)
                    cat <<'EOF'
Install Docker via your distribution's package manager (look for docker.io
or docker-ce). Then add yourself to the docker group and start the daemon:

    sudo usermod -aG docker "$USER"
    sudo systemctl enable --now docker

Log out and back in for the group change to apply.
EOF
                    ;;
            esac
            ;;
        *)
            cat <<'EOF'
Install Docker for your platform: https://docs.docker.com/get-docker/
EOF
            ;;
    esac

    cat <<'EOF'

Alternatively, bypass Docker entirely by pointing the build at a local MySQL:

    export QUARKUS_DATASOURCE_JDBC_URL=jdbc:mysql://localhost:3306/securedoc
    export QUARKUS_DATASOURCE_USERNAME=securedoc_user
    export QUARKUS_DATASOURCE_PASSWORD=...
EOF
}

if ! command -v docker >/dev/null 2>&1; then
    {
        echo "[securedoc] Docker is not installed."
        echo
        echo "This project uses Docker for:"
        echo "  - Quarkus DevServices (auto-starts a mysql:8 container for dev / test)"
        echo "  - The native-release-pgo profile (docker compose up)"
        echo
        print_install_hint
    } >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    {
        echo "[securedoc] Docker is installed but the daemon is not reachable."
        echo
        echo "Possible causes:"
        echo "  - Docker Desktop / dockerd is not running. Start it."
        echo "  - On Linux, your user is not in the 'docker' group:"
        echo "        sudo usermod -aG docker \"\$USER\""
        echo "    then log out and back in."
        echo "  - With rootless Docker, ensure DOCKER_HOST is set in your shell."
        echo
        echo "To bypass Docker entirely, set QUARKUS_DATASOURCE_JDBC_URL,"
        echo "QUARKUS_DATASOURCE_USERNAME, and QUARKUS_DATASOURCE_PASSWORD."
    } >&2
    exit 1
fi
