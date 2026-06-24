-- ------------------------------------------------------------
-- V6__restore_staff_organization_email_unique.sql
--
-- V1 declared UNIQUE (organization_id, email) on `staff`, which MySQL auto-
-- named after the first column -> `organization_id`. V2's PK type rebuild
-- dropped the BIGINT organization_id column; MySQL reacted by removing that
-- column from the composite index but keeping the index alive on the
-- remaining column (email), still under the name `organization_id`. The
-- net effect is a stale UNIQUE (email) index that prevents the same email
-- from appearing under two different tenants.
--
-- The /admin/{slug}/login flow needs the legitimate composite
-- (organization_id, email) so the same email can legitimately belong to
-- staff in multiple tenants -- the URL slug pins which one. Drop the
-- stale single-column unique and create the proper composite under an
-- explicit, future-rename-friendly name.
-- ------------------------------------------------------------

-- Drop the stale single-column unique left over from V1+V2. Wrapped in a
-- check so the migration is idempotent against schemas that were rebuilt
-- without ever holding the V1 auto-name.
DROP PROCEDURE IF EXISTS _v6_drop_index_if_exists;
DELIMITER //
CREATE PROCEDURE _v6_drop_index_if_exists(IN tbl VARCHAR(64),
                                          IN idx_name VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tbl
          AND INDEX_NAME = idx_name
    ) THEN
        SET @sql := CONCAT('ALTER TABLE `', tbl, '` DROP INDEX `', idx_name, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL _v6_drop_index_if_exists('staff', 'organization_id');
CALL _v6_drop_index_if_exists('staff', 'email');

DROP PROCEDURE _v6_drop_index_if_exists;

-- Defensive: collapse any duplicate (organization_id, LOWER(email)) tuples
-- that could have drifted in while the constraint was missing. Keep the
-- oldest row per tuple so the first registration "owns" the email; if two
-- rows tie on created_at to the microsecond, both survive and the ALTER
-- below fails for human review.
DELETE later_row FROM staff AS later_row
INNER JOIN staff AS earlier_row
    ON later_row.organization_id = earlier_row.organization_id
   AND LOWER(later_row.email) = LOWER(earlier_row.email)
   AND later_row.created_at > earlier_row.created_at;

ALTER TABLE staff
    ADD CONSTRAINT uc_staff_organization_email
        UNIQUE (organization_id, email);
