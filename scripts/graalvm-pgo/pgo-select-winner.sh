#!/usr/bin/env bash
# scripts/graalvm-pgo/pgo-select-winner.sh
#
# Compares ops_per_sec between normal and optimized; copies the higher-
# throughput binary back to the canonical target/securedoc-*-runner path so
# downstream packaging picks it up. Deletes the throwaway instrumented
# binary -- it's pure overhead at runtime.
#
# Exit codes:
#   0 -- winner chosen and copied
#   1 -- metrics files missing or unreadable
#
# STUB: tie-breaking rule -- optimized wins ties (same ops_per_sec means
# slightly smaller binary or warmer cache, not worse). Adjust here if a
# different rule is needed later.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PGO_DIR="$PROJECT_DIR/target/pgo-run"

read_ops() {
    grep -oE '^ops_per_sec=[0-9]+' "$1" 2>/dev/null | head -1 | cut -d= -f2
}

NORMAL_OPS=$(read_ops "$PGO_DIR/normal.metrics" || true)
OPT_OPS=$(read_ops "$PGO_DIR/optimized.metrics" || true)

if [[ -z "$NORMAL_OPS" || -z "$OPT_OPS" ]]; then
    echo "FATAL: missing metrics (normal=$NORMAL_OPS, optimized=$OPT_OPS)" >&2
    exit 1
fi

if [[ "$OPT_OPS" -ge "$NORMAL_OPS" ]]; then
    WINNER=optimized
else
    WINNER=normal
fi

echo "===== pgo-select-winner: $WINNER wins (normal=$NORMAL_OPS, optimized=$OPT_OPS ops/sec) ====="

WINNER_BIN="$(ls "$PROJECT_DIR/target/securedoc-"*"-runner-$WINNER" 2>/dev/null | head -1)"
if [[ -z "$WINNER_BIN" ]]; then
    echo "FATAL: winner binary not found: target/securedoc-*-runner-$WINNER" >&2
    exit 1
fi

CANONICAL="$(echo "$WINNER_BIN" | sed -E 's/-runner-(normal|optimized)$/-runner/')"
cp -f "$WINNER_BIN" "$CANONICAL"
echo "===== pgo-select-winner: copied -> $CANONICAL ====="

# Drop the instrumented binary; it's load-bearing only for default.iprof generation
rm -f "$PROJECT_DIR/target/securedoc-"*"-runner-instrumented" 2>/dev/null || true
