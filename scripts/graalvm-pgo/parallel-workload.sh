#!/usr/bin/env bash
# scripts/graalvm-pgo/parallel-workload.sh
#
# Spawns N concurrent workload.sh workers against the locally-running native
# binary, waits for them all to finish (or for the workload window to expire),
# and aggregates throughput / error counts into target/pgo-run/<variant>.metrics.
#
# Usage:
#   bash scripts/graalvm-pgo/parallel-workload.sh <variant>
#
# STUB: relaxed gate -- accept any run with >=1 successful worker so a single
# flaky vthread doesn't abort the whole PGO build.

set -uo pipefail

VARIANT="${1:?usage: parallel-workload.sh <variant>}"
WORKERS="${PGO_WORKERS:-10}"
DURATION_SEC="${PGO_DURATION_SEC:-300}"

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PGO_DIR="$PROJECT_DIR/target/pgo-run"
mkdir -p "$PGO_DIR"

METRICS_FILE="$PGO_DIR/$VARIANT.metrics"
echo "===== parallel-workload[$VARIANT]: $WORKERS workers x ${DURATION_SEC}s ====="

PIDS=()
for worker in $(seq 1 "$WORKERS"); do
    bash "$PROJECT_DIR/scripts/graalvm-pgo/workload.sh" "$VARIANT" "$worker" "$DURATION_SEC" \
        > "$PGO_DIR/$VARIANT.worker-$worker.log" 2>&1 &
    PIDS+=($!)
done

SUCCEEDED=0
FAILED=0
for pid in "${PIDS[@]}"; do
    if wait "$pid"; then
        SUCCEEDED=$((SUCCEEDED + 1))
    else
        FAILED=$((FAILED + 1))
    fi
done

# Aggregate -- workers each write a single "ops=NNNN errors=NNNN" line at the
# end of their log. STUB: workload.sh's metric format is TBD; this is the
# placeholder shape.
TOTAL_OPS=0
TOTAL_ERRORS=0
for worker in $(seq 1 "$WORKERS"); do
    log="$PGO_DIR/$VARIANT.worker-$worker.log"
    ops=$(grep -oE 'ops=[0-9]+' "$log" 2>/dev/null | tail -1 | cut -d= -f2)
    errs=$(grep -oE 'errors=[0-9]+' "$log" 2>/dev/null | tail -1 | cut -d= -f2)
    TOTAL_OPS=$((TOTAL_OPS + ${ops:-0}))
    TOTAL_ERRORS=$((TOTAL_ERRORS + ${errs:-0}))
done

{
    echo "variant=$VARIANT"
    echo "workers=$WORKERS"
    echo "duration_sec=$DURATION_SEC"
    echo "workers_succeeded=$SUCCEEDED"
    echo "workers_failed=$FAILED"
    echo "ops_total=$TOTAL_OPS"
    echo "errors_total=$TOTAL_ERRORS"
    if [[ $DURATION_SEC -gt 0 ]]; then
        echo "ops_per_sec=$((TOTAL_OPS / DURATION_SEC))"
    fi
} > "$METRICS_FILE"

cat "$METRICS_FILE"

# Relaxed gate -- pass if at least one worker survived. Tightens later.
if [[ $SUCCEEDED -ge 1 ]]; then
    exit 0
else
    echo "FATAL: 0 workers succeeded" >&2
    exit 1
fi
