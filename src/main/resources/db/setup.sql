-- ------------------------------------------------------------
-- setup.sql -- One-time database, role, and user provisioning for SecureDoc.
--
-- Run once per MySQL 8 instance (local dev box, staging server, prod):
--
--   mysql -u root -p < src/main/resources/db/setup.sql
--
-- Idempotent: uses MySQL 8's IF NOT EXISTS variants so re-running is safe.
-- Flyway-managed schema migrations (V1__init.sql, V2__..., ...) are applied
-- separately at app startup (quarkus.flyway.migrate-at-start=true) or by a
-- dedicated deployment step.
--
-- Privilege model (least-privilege, three roles):
--
--   securedoc_schema_admin -- DDL + DML on the securedoc schema. Used by
--                             Flyway when it runs migrations. If Quarkus
--                             auto-migrates at boot, the app user must be
--                             granted this role. In a CI/CD setup that
--                             pre-applies migrations, only the deployment
--                             job needs it.
--   securedoc_app          -- DML only (SELECT / INSERT / UPDATE / DELETE
--                             plus EXECUTE for routines). The Quarkus
--                             runtime should connect as this role once
--                             migrations have been applied separately.
--   securedoc_readonly     -- SELECT only. For monitoring, reports, and
--                             ad-hoc inspection without write risk.
--
-- Three corresponding users are provisioned. Production deployments must
-- rotate the committed dev passwords below via env-var overrides on the
-- CREATE USER / ALTER USER statements (or run a separate prod-only
-- bootstrap script). Never ship the committed passwords to a public host.
-- ------------------------------------------------------------

-- -------------------------------------------------------
-- 1. Database
-- -------------------------------------------------------
CREATE DATABASE IF NOT EXISTS securedoc
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- -------------------------------------------------------
-- 2. Roles (MySQL 8.0+)
-- -------------------------------------------------------
CREATE ROLE IF NOT EXISTS 'securedoc_schema_admin', 'securedoc_app', 'securedoc_readonly';

-- securedoc_schema_admin -- DDL + DML.
GRANT
    CREATE, ALTER, DROP, INDEX, REFERENCES,
    CREATE VIEW, SHOW VIEW,
    CREATE ROUTINE, ALTER ROUTINE, EXECUTE,
    TRIGGER, EVENT,
    SELECT, INSERT, UPDATE, DELETE,
    CREATE TEMPORARY TABLES, LOCK TABLES
ON securedoc.* TO 'securedoc_schema_admin';

-- securedoc_app -- DML only. Sufficient for the Quarkus runtime when
-- migrations are applied out-of-band by a deployment job.
GRANT
    SELECT, INSERT, UPDATE, DELETE,
    EXECUTE,
    LOCK TABLES
ON securedoc.* TO 'securedoc_app';

-- securedoc_readonly -- SELECT only.
GRANT SELECT ON securedoc.* TO 'securedoc_readonly';

-- -------------------------------------------------------
-- 3. Users
--
-- Passwords committed below are dev/local-loopback defaults. Rotate them
-- on any host reachable from outside localhost. The Quarkus runtime reads
-- credentials from QUARKUS_DATASOURCE_USERNAME / QUARKUS_DATASOURCE_PASSWORD
-- env vars in prod, so a fresh prod box can use a freshly-generated password
-- without changing this file.
--
-- Host scope is 'localhost' for local dev. For a remote DB on a private
-- network, change to '%' or the specific app-host CIDR.
-- -------------------------------------------------------

-- Migration / schema-admin user. Used by Flyway -- and by Quarkus' boot-time
-- migrate-at-start path if you wire the same datasource for both.
CREATE USER IF NOT EXISTS 'securedoc_admin'@'localhost'
    IDENTIFIED BY 'securedoc_admin!CHANGEME_dev_only'
    PASSWORD EXPIRE NEVER;
GRANT 'securedoc_schema_admin' TO 'securedoc_admin'@'localhost';

-- Runtime app user. Use this for the Quarkus app in production where
-- migrations are pre-applied by a separate deployment step.
CREATE USER IF NOT EXISTS 'securedoc_user'@'localhost'
    IDENTIFIED BY 'securedoc!123'
    PASSWORD EXPIRE NEVER;
GRANT 'securedoc_app' TO 'securedoc_user'@'localhost';

-- Read-only user for monitoring, reports, and ad-hoc inspection.
CREATE USER IF NOT EXISTS 'securedoc_reader'@'localhost'
    IDENTIFIED BY 'securedoc_reader!CHANGEME_dev_only'
    PASSWORD EXPIRE NEVER;
GRANT 'securedoc_readonly' TO 'securedoc_reader'@'localhost';

-- -------------------------------------------------------
-- 4. Default roles -- so the granted privileges are active at login
--    without each session having to SET ROLE explicitly. The JDBC driver
--    doesn't issue SET ROLE on its own; without DEFAULT ROLE ALL the
--    connection would have no effective privileges.
-- -------------------------------------------------------
SET DEFAULT ROLE ALL TO
    'securedoc_admin'@'localhost',
    'securedoc_user'@'localhost',
    'securedoc_reader'@'localhost';

-- -------------------------------------------------------
-- 5. Dev/prod parity: `securedoc_user` holds ONLY the DML-bearing
--    `securedoc_app` role in every environment. Flyway authenticates
--    separately as `securedoc_admin` -- in dev via the %dev.quarkus.flyway.*
--    keys in application.properties, in prod via QUARKUS_FLYWAY_* env vars
--    set by the deployment job. The runtime datasource never holds DDL
--    privileges, so a Flyway-checksum mismatch can't accidentally drop
--    tables from a running app.
-- -------------------------------------------------------

FLUSH PRIVILEGES;
