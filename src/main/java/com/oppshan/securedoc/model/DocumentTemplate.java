package com.oppshan.securedoc.model;

import com.oppshan.securedoc.dto.DocumentTemplateView;
import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Per-organization issuable document template. The dropdown on
 * /user/request.xhtml lists the active rows scoped to the picked
 * organization; the {@code template_data} BLOB is only read at
 * issuance time and by the preview servlet. See the field's
 * comment for why it's eagerly fetched.
 */
@Entity
@Table(name = "document_templates")
public class DocumentTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 4321987651039283746L;

    public enum DocType {
        BARANGAY_CLEARANCE,
        CERTIFICATE_OF_RESIDENCY,
        CERTIFICATE_OF_INDIGENCY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Convert(converter = DocTypeConverter.class)
    @Column(name = "doc_type", nullable = false,
            columnDefinition = "ENUM('barangay_clearance','certificate_of_residency','certificate_of_indigency') NOT NULL")
    private DocType docType;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Eagerly loaded: Jakarta Data repos run on a StatelessSession that
    // closes when the method returns, so @Basic(fetch=LAZY) here throws
    // LazyInitializationException when the service later reads the blob.
    // TODO at scale: switch list/dropdown queries to a JPQL constructor-
    // expression projection (id, doc_type, name, description) so the
    // blob column is never selected for those reads.
    @Column(name = "template_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] templateData;

    @Column(name = "mime_type", length = 50)
    private String mimeType = "application/pdf";

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public DocumentTemplate() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public DocType getDocType() {
        return docType;
    }

    public void setDocType(DocType docType) {
        this.docType = docType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getTemplateData() {
        return templateData;
    }

    public void setTemplateData(byte[] templateData) {
        this.templateData = templateData;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public DocumentTemplateView toView() {
        DocumentTemplateView view = new DocumentTemplateView();
        view.setId(id);
        view.setDocType(docType);
        view.setName(name);
        view.setDescription(description);
        view.setCreatedAt(createdAt);
        return view;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentTemplate)) return false;
        return Objects.equals(id, ((DocumentTemplate) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Bridges {@link DocType} (uppercase JVM convention) with the lowercase
     * MySQL ENUM in document_templates.doc_type. Mirrors Staff.RoleConverter.
     */
    @Converter
    public static class DocTypeConverter implements AttributeConverter<DocType, String> {

        @Override
        public String convertToDatabaseColumn(DocType type) {
            return type == null ? null : type.name().toLowerCase();
        }

        @Override
        public DocType convertToEntityAttribute(String s) {
            return s == null ? null : DocType.valueOf(s.toUpperCase());
        }
    }
}
