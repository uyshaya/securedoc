USE securedoc;

--  1. STAFF / ADMIN
CREATE TABLE staff
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    middle_name   VARCHAR(100),
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('staff', 'admin') DEFAULT 'staff',
    is_active     BOOLEAN   DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login    DATETIME
);

--  2. STAFF OTP  (login 2FA)
CREATE TABLE staff_otps
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id     BIGINT     NOT NULL,
    otp_code     VARCHAR(6) NOT NULL,
    otp_type     ENUM('login', 'password_reset') DEFAULT 'login',
    otp_attempts INT       DEFAULT 0,
    is_used      BOOLEAN   DEFAULT FALSE,
    expires_at   DATETIME   NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE CASCADE
);


--  3. RESIDENT OTP  (email verification before submitting)
CREATE TABLE resident_otps
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    otp_code     VARCHAR(6)   NOT NULL,
    otp_attempts INT       DEFAULT 0,
    is_used      BOOLEAN   DEFAULT FALSE,
    expires_at   DATETIME     NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--  4. ORGANIZATION CERTIFICATE
CREATE TABLE org_certificates
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    issuer_name     VARCHAR(255) NOT NULL,
    serial_number   VARCHAR(255) NOT NULL UNIQUE,
    public_key      TEXT         NOT NULL, -- store public key separately for verifier portal
    certificate_pem TEXT         NOT NULL, -- certificate_pem (PEM format)
    expiry_date     DATETIME     NOT NULL,
    is_active       BOOLEAN   DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--  5. DOCUMENT TEMPLATES
CREATE TABLE document_templates
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_type      ENUM('barangay_clearance',
                       'certificate_of_residency',
                       'certificate_of_indigency') NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    template_data LONGBLOB     NOT NULL,
    mime_type     VARCHAR(50) DEFAULT 'application/pdf',
    is_active     BOOLEAN     DEFAULT TRUE, -- allow deactivating old templates
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

--  6. REQUESTERS  (resident personal info per submission)
CREATE TABLE requesters
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    middle_name    VARCHAR(100),
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL, -- NOT UNIQUE: same person may submit multiple times
    sex            ENUM('M', 'F') NOT NULL,
    date_of_birth  DATE         NOT NULL,
    contact_number VARCHAR(20),
    id_type        VARCHAR(50),
    id_image_path  VARCHAR(500), -- file path (store files on disk/S3?, not in DB for better performance)
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--  7. REQUESTS
CREATE TABLE requests
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_number VARCHAR(20) NOT NULL UNIQUE,
    requester_id     BIGINT      NOT NULL,
    template_id      BIGINT      NOT NULL,
    processed_by     BIGINT,
    status           ENUM('pending',
                          'under_review',
                          'processing',
                          'completed',
                          'rejected') DEFAULT 'pending',
    purpose          VARCHAR(255),
    other_purpose    TEXT,
    request_note     TEXT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES requesters (id),
    FOREIGN KEY (template_id) REFERENCES document_templates (id),
    FOREIGN KEY (processed_by) REFERENCES staff (id)
);

--  8. ISSUED DOCUMENTS  (tamper-evident layer)
CREATE TABLE documents
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id         BIGINT       NOT NULL UNIQUE,
    org_certificate_id BIGINT       NOT NULL,
    issued_by          BIGINT       NOT NULL, -- which staff member approved it
    file_name          VARCHAR(255) NOT NULL,
    document_data      LONGBLOB     NOT NULL, -- PDF bytes
    file_size          INT,
    file_hash          VARCHAR(64)  NOT NULL, -- SHA-256 hash of document_data (tamper detection)
    digital_signature  TEXT, -- signature over file_hash
    verification_token VARCHAR(128) NOT NULL UNIQUE, -- encoded as the QR image
    issued_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (request_id) REFERENCES requests (id),
    FOREIGN KEY (org_certificate_id) REFERENCES org_certificates (id),
    FOREIGN KEY (issued_by) REFERENCES staff (id)
);