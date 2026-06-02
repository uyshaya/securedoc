-- ------------------------------------------------------------
-- V5__resident_table.sql
--
-- Resident masterlist. Per-organization directory of residents
-- populated by CSV upload (admin -> /admin/residents/...). Acts
-- as the bootstrap data source until an upstream barangay-record
-- API exists; when that lands, the table is dropped and the
-- service swaps to remote lookups.
--
-- Not joined from any other table -- the request flow snapshots
-- residents into the requester row at submission time, so this
-- table is purely a reference directory.
-- ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS resident
(
    id              CHAR(36)     NOT NULL,
    organization_id CHAR(36)     NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    middle_name     VARCHAR(100) NULL,
    last_name       VARCHAR(100) NOT NULL,
    sex             ENUM('M','F') NOT NULL,
    date_of_birth   DATE         NOT NULL,
    address         VARCHAR(500) NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_resident_organization
        FOREIGN KEY (organization_id) REFERENCES organization (id),
    INDEX idx_resident_organization_lastname_firstname
        (organization_id, last_name, first_name)
);
