-- ------------------------------------------------------------
-- V3__requester_id_image_blob.sql
--
-- Replace the id_image_path VARCHAR with id_image_data LONGBLOB so the
-- resident's uploaded valid ID is persisted inline on the requester row,
-- matching how document_template.template_data holds the PDF bytes.
-- ------------------------------------------------------------

ALTER TABLE requester
    DROP COLUMN id_image_path,
    ADD COLUMN id_image_data LONGBLOB NULL AFTER id_type;
