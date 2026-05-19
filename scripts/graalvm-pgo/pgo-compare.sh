#!/usr/bin/env bash
# scripts/graalvm-pgo/pgo-compare.sh
#
# Reads target/pgo-run/{normal,instrumented,optimized}.metrics and prints a
# side-by-side comparison table. Exits 0 regardless of which variant won --
# pgo-select-winner.sh is the one that picks.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PGO_DIR="$PROJECT_DIR/target/pgo-run"

field() {
    local file="$1" key="$2"
    grep -oE "^$key=[0-9]+" "$file" 2>/dev/null | head -1 | cut -d= -f2 || echo "-"
}

printf "%-15s %12s %12s %12s\n" "metric" "normal" "instrumented" "optimized"
printf "%s\n" "----------------------------------------------------------"

for metric in ops_total errors_total ops_per_sec workers_succeeded; do
    n=$(field "$PGO_DIR/normal.metrics"       "$metric")
    i=$(field "$PGO_DIR/instrumented.metrics" "$metric")
    o=$(field "$PGO_DIR/optimized.metrics"    "$metric")
    printf "%-15s %12s %12s %12s\n" "$metric" "${n:--}" "${i:--}" "${o:--}"
done
