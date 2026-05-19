#!/usr/bin/env bash
# scripts/graalvm-pgo/pgo-reset-db.sh
#
# Truncates app tables between PGO test runs so each binary launches against
# an equivalent empty-app state. Runs against the dockerized MySQL 8 started
# by docker-compose.yml in this directory.
#
# Idempotent: SET FOREIGN_KEY_CHECKS = 0 around the TRUNCATEs so order doesn't
# matter. Leaves `flyway_schema_history` alone -- Flyway should not re-apply
# migrations on the next binary boot.
#
# STUB: table list below is the current snapshot (V1 + V2 applied). When new
# Flyway migrations add tables, update this list. Audit table list against
# `information_schema.tables` if drift is suspected.

set -euo pipefail

MYSQL_CONTAINER="${MYSQL_CONTAINER:-securedoc-pgo-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-securedoc}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-securedoc-pgo-root}"

# Order: leaf tables first so FK_CHECKS-off isn't strictly needed, but we set
# it anyway to defend against table-list reshuffling.
TABLES=(
    staff_otp
    document_template
    staff
    organization
)

SQL="SET FOREIGN_KEY_CHECKS = 0;"
for table in "${TABLES[@]}"; do
    SQL="$SQL TRUNCATE TABLE \`${table}\`;"
done
SQL="$SQL SET FOREIGN_KEY_CHECKS = 1;"

echo "===== pgo-reset-db: truncating ${#TABLES[@]} tables in ${MYSQL_DATABASE} ====="
docker exec -i "$MYSQL_CONTAINER" mysql \
    -u root -p"$MYSQL_ROOT_PASSWORD" \
    "$MYSQL_DATABASE" -e "$SQL"
