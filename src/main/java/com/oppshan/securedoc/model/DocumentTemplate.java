package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.common.AuditableEntity;
import com.oppshan.securedoc.common.AuditableEntityEntityListener;
import com.oppshan.securedoc.dto.DocumentTemplateView;
import jakarta.annotation.Nullable;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
 * Per-organization issuable document template. The dropdown on
 * /user/request.xhtml lists the active rows scoped to the picked
 * organization; the {@code template_data} BLOB is only read at
 * issuance, not for the picker -- fetch is LAZY so listing the
 * dropdown doesn't pull MB-sized PDF bytes.
 */
@Entity
@EntityListeners({
        AuditableEntityEntityListener.class
})
@Table(name = "document_template",
        indexes = {
                @Index(
                        name = "idx_document_templates_organization_id",
                        columnList = "organization_id"
                ),
                @Index(
                        name = "idx_document_templates_doc_type",
                        columnList = "doc_type"
                )
        }
)
public class DocumentTemplate
        implements AuditableEntity<DocumentTemplate>, Serializable {

    @Serial
    private static final long serialVersionUID = 4321987651039283746L;

    public enum DocType {
        BARANGAY_CLEARANCE,
        CERTIFICATE_OF_RESIDENCY,
        CERTIFICATE_OF_INDIGENCY
    }

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
            targetEntity = Organization.class
    )
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL"
    )
    @NotNull
    private Organization organization;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type",
            nullable = false,
            updatable = false,
            columnDefinition = "ENUM('BARANGAY_CLEARANCE','CERTIFICATE_OF_RESIDENCY','CERTIFICATE_OF_INDIGENCY') NOT NULL")
    @NotNull
    private DocType docType;

    @Basic(optional = false)
    @Column(name = "name",
            nullable = false)
    @NotEmpty
    private String name;

    @Column(name = "description",
            columnDefinition = "TEXT")
    @Nullable
    private String description;

    @Basic(fetch = FetchType.LAZY, optional = false)
    @Column(name = "template_data",
            nullable = false,
            columnDefinition = "LONGBLOB")
    @NotNull
    private byte[] templateData;

    @Column(name = "mime_type",
            length = 50)
    @Nullable
    private String mimeType = "application/pdf";

    @Basic(optional = false)
    @Column(name = "active",
            nullable = false)
    private boolean active = true;

    @Basic(optional = false)
    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    @NotNull
    private Instant createdAt;

    @Basic(optional = false)
    @Column(name = "updated_at",
            nullable = false)
    @NotNull
    private Instant lastModifiedAt;

    public DocumentTemplate() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public DocumentTemplate setId(UUID id) {
        this.id = id;
        return this;
    }

    public Organization getOrganization() {
        return organization;
    }

    public DocumentTemplate setOrganization(Organization organization) {
        this.organization = organization;
        return this;
    }

    public DocType getDocType() {
        return docType;
    }

    public DocumentTemplate setDocType(DocType docType) {
        this.docType = docType;
        return this;
    }

    public String getName() {
        return name;
    }

    public DocumentTemplate setName(String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public DocumentTemplate setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    public byte[] getTemplateData() {
        return templateData;
    }

    public DocumentTemplate setTemplateData(byte[] templateData) {
        this.templateData = templateData;
        return this;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    public DocumentTemplate setMimeType(@Nullable String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public boolean isActive() {
        return active;
    }

    public DocumentTemplate setActive(boolean active) {
        this.active = active;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public DocumentTemplate setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public DocumentTemplate setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    public DocumentTemplateView toView() {
        return new DocumentTemplateView()
                .setId(id)
                .setDocType(docType)
                .setName(name)
                .setDescription(description);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final DocumentTemplate that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(organization, that.organization) &&
               docType == that.docType &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               Objects.equals(mimeType, that.mimeType) &&
               active == that.active &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastModifiedAt, that.lastModifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                organization,
                docType,
                name,
                description,
                mimeType,
                active,
                createdAt,
                lastModifiedAt
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("organization", organization)
                .add("docType", docType)
                .add("name", name)
                .add("description", description)
                .add("mimeType", mimeType)
                .add("active", active)
                .add("createdAt", createdAt)
                .add("lastModifiedAt", lastModifiedAt)
                .toString();
    }
}
