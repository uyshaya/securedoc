-- 01-grants.sql -- grants the load-test app user enough privileges to run
-- Flyway migrations and CRUD. Applied once on first container boot via
-- /docker-entrypoint-initdb.d.
--
-- STUB: this is a minimum-viable grant for PGO load testing only -- it does
-- NOT mirror the dev/prod three-role model from src/main/resources/db/setup.sql
-- (securedoc_schema_admin / securedoc_app / securedoc_readonly). The PGO
-- workload is single-purpose and short-lived, so an "all privileges on the
-- securedoc schema" grant for securedoc_user is enough. If the PGO setup
-- ever needs to exercise the privilege boundary, port the three-role setup
-- here.

GRANT ALL PRIVILEGES ON securedoc.* TO 'securedoc_user'@'%';
FLUSH PRIVILEGES;
