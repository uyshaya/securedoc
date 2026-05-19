#!/usr/bin/env pwsh
# scripts/graalvm-pgo/pgo-reset-db.ps1
#
# PowerShell counterpart to pgo-reset-db.sh. Truncates app tables between PGO
# test runs so each binary launches against an equivalent empty-app state.
#
# STUB: keep TABLES in lockstep with pgo-reset-db.sh when Flyway migrations
# add or remove app tables.

$MysqlContainer    = if ($env:MYSQL_CONTAINER)     { $env:MYSQL_CONTAINER }     else { 'securedoc-pgo-mysql' }
$MysqlDatabase     = if ($env:MYSQL_DATABASE)      { $env:MYSQL_DATABASE }      else { 'securedoc' }
$MysqlRootPassword = if ($env:MYSQL_ROOT_PASSWORD) { $env:MYSQL_ROOT_PASSWORD } else { 'securedoc-pgo-root' }

$Tables = @(
    'staff_otp',
    'document_template',
    'staff',
    'organization'
)

$sql = 'SET FOREIGN_KEY_CHECKS = 0;'
foreach ($table in $Tables) {
    $sql += " TRUNCATE TABLE `$table`;"
}
$sql += ' SET FOREIGN_KEY_CHECKS = 1;'

Write-Host "===== pgo-reset-db: truncating $($Tables.Count) tables in $MysqlDatabase ====="
docker exec -i $MysqlContainer mysql -u root -p"$MysqlRootPassword" $MysqlDatabase -e $sql
