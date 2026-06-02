-- ------------------------------------------------------------
-- V4: relax document.org_certificate_id to NULL
-- ------------------------------------------------------------
-- The full design (V1 + V2) requires every issued document to reference an
-- org_certificate row that holds the leaf public key + cert used to sign it.
-- That column stays in place, but it must be nullable so we can persist
-- Document rows BEFORE the PKI signing module is implemented.
--
-- When signing lands the column flips back to NOT NULL and the existing
-- nullable rows are backfilled. The FK constraint stays unchanged -- MySQL
-- already allows NULL values in FK columns.
-- ------------------------------------------------------------

ALTER TABLE document
    MODIFY COLUMN org_certificate_id CHAR(36) NULL;
