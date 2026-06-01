package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.dto.DocumentDownloadView;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A persisted, issued PDF document tied 1:1 to a {@link Request}. Created
 * when staff approves a request on {@code /admin/requests.xhtml}; the
 * {@link #documentData} bytes are exactly what the resident downloads from
 * {@code /admin/requests/document}. Today those bytes are the uploaded
 * {@link DocumentTemplate}'s {@code templateData} verbatim (pass-through);
 * once the marker-rendering and PKI signing tickets land they will be the
 * rendered + signed output instead.
 *
 * <p>{@link #orgCertificateId} and {@link #digitalSignature} are temporarily
 * nullable placeholders for the PKI signing flow; migration V4 relaxes the
 * {@code org_certificate_id} column so the row can persist before signing
 * exists. {@link #verificationToken} is minted now (opaque random UUID)
 * so the verifier-portal ticket can wire QR lookups without a second schema
 * change.
 */
@Entity
@Table(name = "document",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_document_request",
                        columnNames = {"request_id"}
                ),
                @UniqueConstraint(
                        name = "uc_document_verification_token",
                        columnNames = {"verification_token"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_document_request_id",
                        columnList = "request_id"
                ),
                @Index(
                        name = "idx_document_issued_by",
                        columnList = "issued_by"
                ),
                @Index(
                        name = "idx_document_org_certificate_id",
                        columnList = "org_certificate_id"
                )
        }
)
public class Document implements Serializable {

    @Serial
    private static final long serialVersionUID = 8462910374261854293L;

    @Id
    @Basic(optional = false)
    @Column(name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL")
    @JdbcTypeCode(SqlTypes.CHAR)
    @UuidGenerator(style = Style.VERSION_7)
    @NotNull
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            targetEntity = Request.class
    )
    @JoinColumn(
            name = "request_id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL"
    )
    @NotNull
    private Request request;

    // Raw UUID column (no @ManyToOne) until OrgCertificate is modelled by the
    // PKI signing module. Nullable per migration V4 -- see class javadoc.
    @Column(name = "org_certificate_id",
            columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Nullable
    private UUID orgCertificateId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            targetEntity = Staff.class
    )
    @JoinColumn(
            name = "issued_by",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL"
    )
    @NotNull
    private Staff issuedBy;

    @Basic(optional = false)
    @Column(name = "file_name",
            nullable = false,
            length = 255)
    @NotEmpty
    private String fileName;

    @Basic(optional = false)
    @Column(name = "document_data",
            nullable = false,
            columnDefinition = "LONGBLOB")
    @NotNull
    private byte[] documentData;

    @Column(name = "file_size")
    @Nullable
    private Integer fileSize;

    @Basic(optional = false)
    @Column(name = "file_hash",
            nullable = false,
            length = 64)
    @NotEmpty
    private String fileHash;

    @Column(name = "digital_signature",
            columnDefinition = "TEXT")
    @Nullable
    private String digitalSignature;

    @Basic(optional = false)
    @Column(name = "verification_token",
            nullable = false,
            unique = true,
            length = 128)
    @NotEmpty
    private String verificationToken;

    @Basic(optional = false)
    @Column(name = "issued_at",
            nullable = false,
            updatable = false)
    @NotNull
    private Instant issuedAt;

    public Document() {
    }

    public UUID getId() {
        return id;
    }

    public Document setId(UUID id) {
        this.id = id;
        return this;
    }

    public Request getRequest() {
        return request;
    }

    public Document setRequest(Request request) {
        this.request = request;
        return this;
    }

    @Nullable
    public UUID getOrgCertificateId() {
        return orgCertificateId;
    }

    public Document setOrgCertificateId(@Nullable UUID orgCertificateId) {
        this.orgCertificateId = orgCertificateId;
        return this;
    }

    public Staff getIssuedBy() {
        return issuedBy;
    }

    public Document setIssuedBy(Staff issuedBy) {
        this.issuedBy = issuedBy;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public Document setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public byte[] getDocumentData() {
        return documentData;
    }

    public Document setDocumentData(byte[] documentData) {
        this.documentData = documentData;
        return this;
    }

    @Nullable
    public Integer getFileSize() {
        return fileSize;
    }

    public Document setFileSize(@Nullable Integer fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public String getFileHash() {
        return fileHash;
    }

    public Document setFileHash(String fileHash) {
        this.fileHash = fileHash;
        return this;
    }

    @Nullable
    public String getDigitalSignature() {
        return digitalSignature;
    }

    public Document setDigitalSignature(@Nullable String digitalSignature) {
        this.digitalSignature = digitalSignature;
        return this;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public Document setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
        return this;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Document setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public DocumentDownloadView toDownloadView() {
        return new DocumentDownloadView()
                .setFileName(fileName)
                .setDocumentData(documentData);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final Document that)) {
            return false;
        }

        // documentData (LONGBLOB) intentionally excluded -- equality on
        // multi-MB byte arrays is wasteful and the id is the canonical
        // discriminator anyway.
        return Objects.equals(id, that.id) &&
                Objects.equals(request, that.request) &&
                Objects.equals(orgCertificateId, that.orgCertificateId) &&
                Objects.equals(issuedBy, that.issuedBy) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(fileSize, that.fileSize) &&
                Objects.equals(fileHash, that.fileHash) &&
                Objects.equals(digitalSignature, that.digitalSignature) &&
                Objects.equals(verificationToken, that.verificationToken) &&
                Objects.equals(issuedAt, that.issuedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                request,
                orgCertificateId,
                issuedBy,
                fileName,
                fileSize,
                fileHash,
                digitalSignature,
                verificationToken,
                issuedAt
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("fileName", fileName)
                .add("fileHash", fileHash)
                .add("verificationToken", verificationToken)
                .add("issuedAt", issuedAt)
                .toString();
    }
}
