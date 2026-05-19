#!/usr/bin/env bash
# scripts/graalvm-pgo/pgo-test-binary.sh
#
# Boots one of the three native binaries (normal / instrumented / optimized),
# drives parallel-workload.sh against it for the configured workload window,
# captures throughput metrics into target/pgo-run/<variant>.metrics, and
# (for the instrumented variant only) shuts down cleanly so GraalVM writes
# default.iprof to target/pgo-run/.
#
# Usage:
#   bash scripts/graalvm-pgo/pgo-test-binary.sh <variant>
#     <variant> := normal | instrumented | optimized
#
# Expected by the Maven profile (`native-release-pgo` -- see README.md TODO):
#   - <variant>-runner binary saved at target/securedoc-*-runner-<variant>
#   - MySQL container already up via the profile's prepare-package phase
#
# STUB: securedoc has no Keycloak / OIDC. Auth is email + OTP via the staff_otp
# table. The workload script (workload.sh) needs to seed an admin row and
# pluck OTPs out of the DB to drive the login flow -- see workload.sh TODO.

set -euo pipefail

VARIANT="${1:?usage: pgo-test-binary.sh <normal|instrumented|optimized>}"
case "$VARIANT" in
    normal|instrumented|optimized) ;;
    *) echo "unknown variant: $VARIANT (expected normal | instrumented | optimized)" >&2; exit 2;;
esac

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PGO_DIR="$PROJECT_DIR/target/pgo-run"
mkdir -p "$PGO_DIR"

BINARY="$(ls "$PROJECT_DIR/target/securedoc-"*"-runner-$VARIANT" 2>/dev/null | head -1)"
if [[ -z "$BINARY" ]]; then
    echo "no binary found for variant=$VARIANT at target/securedoc-*-runner-$VARIANT" >&2
    exit 1
fi

# Prod-matched JVM/runtime flags. -Xmx512m caps heap to match the t4g.small
# deploy target. Virtual-thread-per-task pool size tracks vCPU count.
echo "===== pgo-test-binary[$VARIANT]: launching $BINARY ====="
"$BINARY" \
    -Xmx512m \
    -Djdk.virtualThreadScheduler.parallelism="$(nproc)" \
    > "$PGO_DIR/$VARIANT.app.log" 2>&1 &
APP_PID=$!
trap "kill -TERM $APP_PID 2>/dev/null || true" EXIT INT TERM

# Wait for /q/health/ready (Quarkus default). TODO: confirm health endpoint
# is enabled in securedoc -- if not, add quarkus-smallrye-health and probe
# /q/health/ready instead of polling the index page.
echo "===== pgo-test-binary[$VARIANT]: waiting for app ready ====="
for _ in $(seq 1 60); do
    if curl -sf "http://localhost:8080/" >/dev/null 2>&1; then
        echo "===== pgo-test-binary[$VARIANT]: app ready ====="
        break
    fi
    sleep 1
done

# Drive the workload
bash "$PROJECT_DIR/scripts/graalvm-pgo/parallel-workload.sh" "$VARIANT"
WORKLOAD_RC=$?

# Graceful shutdown so the instrumented variant flushes default.iprof
echo "===== pgo-test-binary[$VARIANT]: shutting down app ====="
kill -TERM $APP_PID 2>/dev/null || true
wait $APP_PID 2>/dev/null || true
trap - EXIT INT TERM

# Instrumented runs leave default.iprof in CWD. Move it to target/pgo-run/
if [[ "$VARIANT" == "instrumented" ]]; then
    if [[ -f default.iprof ]]; then
        mv default.iprof "$PGO_DIR/default.iprof"
        echo "===== pgo-test-binary[instrumented]: default.iprof captured ($(wc -c <"$PGO_DIR/default.iprof") bytes) ====="
    else
        echo "WARNING: instrumented run produced no default.iprof" >&2
    fi
fi

exit "$WORKLOAD_RC"
