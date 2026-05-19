#!/usr/bin/env bash
# scripts/graalvm-pgo/workload.sh
#
# Per-worker workload loop. Drives the locally-running native binary for
# DURATION_SEC seconds, then writes a single final "ops=NNNN errors=NNNN"
# line to stdout so parallel-workload.sh can aggregate.
#
# Usage:
#   bash scripts/graalvm-pgo/workload.sh <variant> <worker_index> <duration_sec>
#
# STUB -- this script is a placeholder shape only. The actual workload steps
# need to be designed and implemented based on securedoc's auth + request flow.
#
# WHAT NEEDS FILLING IN
# =====================
# securedoc has no Keycloak / OIDC. Auth is email + password + email OTP, and
# the OTP gets sent via SMTP (Gmail app password in dev). For a PGO load test
# we can't drive real SMTP; instead the workload needs to:
#
#   1. Seed an admin row at startup. Either insert directly into the `staff`
#      table via mysql exec, or call a dedicated REST/JSF "seed" endpoint
#      that only exists in the `native-release-pgo` profile. Decision pending.
#
#   2. Drive the JSF login flow with the seeded admin. Two options:
#        a) Skip email OTP for the load test: short-circuit the OTP-required
#           gate when a `securedoc.pgo.load-test=true` system property is set,
#           AND pluck the most recent `staff_otp` row directly from MySQL.
#        b) Use a hardcoded test OTP value in pgo profile builds. Less clean
#           but no auth-flow patching needed.
#
#   3. Hit a mix of representative endpoints. Suggested mix (revisit after the
#      admin / resident flows are wired):
#        - /admin/login.xhtml -> /admin/dashboard.xhtml      (login flow)
#        - /admin/requests.xhtml + /admin/templates.xhtml    (read-heavy nav)
#        - /admin/staff/staff-management.xhtml + table sort  (jsf ajax)
#        - /user/request.xhtml -> POST landing form          (resident flow)
#        - /verifier/verify.xhtml                            (verifier flow)
#
#   4. Track ops + errors. The workload loop reports them at exit.
#
# For now this stub does a placeholder GET against the index page.

set -uo pipefail

VARIANT="${1:?usage: workload.sh <variant> <worker_index> <duration_sec>}"
WORKER="${2:?missing worker_index}"
DURATION_SEC="${3:?missing duration_sec}"

BASE_URL="${SECUREDOC_BASE_URL:-http://localhost:8080}"

deadline=$(( $(date +%s) + DURATION_SEC ))
ops=0
errors=0

echo "===== workload[$VARIANT/w$WORKER]: looping for ${DURATION_SEC}s against $BASE_URL ====="

while [[ $(date +%s) -lt $deadline ]]; do
    # STUB: replace with real auth + endpoint mix once decided. See header.
    if curl -sf -o /dev/null --max-time 5 "$BASE_URL/"; then
        ops=$((ops + 1))
    else
        errors=$((errors + 1))
    fi
done

echo "ops=$ops errors=$errors"
