#!/usr/bin/env bash
# scripts/graalvm-pgo/pgo-build.sh
#
# Canonical entry point for the native-release-pgo Maven build. Wraps
# `./mvnw -P native-release-pgo` with an EXIT/INT/TERM trap that brings the
# docker compose stack down whether the build succeeds, fails, or is
# interrupted (Ctrl-C, SIGTERM). This is the only invocation path that
# guarantees the MySQL container is stopped and its volume purged after the
# build is done, no matter how it ended.
#
# Usage:
#   bash scripts/graalvm-pgo/pgo-build.sh install               # full build + load tests + install
#   bash scripts/graalvm-pgo/pgo-build.sh verify -DskipTests    # what CI would run if/when wired
#
# The pom.xml's compose-down execution in post-integration-test (TODO: not yet
# added to securedoc's pom) handles the happy-path teardown when mvn is run
# directly with no wrapper. This wrapper adds the safety net for failure /
# interruption / kill paths.
#
# What is NOT caught: SIGKILL (kill -9). Bash can't trap SIGKILL. If you nuke
# the wrapper with -9, run `docker compose -f scripts/graalvm-pgo/docker-compose.yml down -v` by hand.
#
# STUB: the `native-release-pgo` Maven profile is not yet defined in pom.xml.
# This wrapper will fail with "no plugin found" until the profile is added.
# See README.md in this directory for the full pipeline design.

set -uo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$PROJECT_DIR/scripts/graalvm-pgo/docker-compose.yml"

teardown() {
    local rc=$?
    echo "" >&2
    echo "===== pgo-build: tearing down docker compose stack (rc=$rc) =====" >&2
    docker compose -f "$COMPOSE_FILE" down -v >&2 || true
    return $rc
}
trap teardown EXIT INT TERM

cd "$PROJECT_DIR"
./mvnw -B -P native-release-pgo "$@"
