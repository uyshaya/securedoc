package com.oppshan.securedoc.dto;

import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Request;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Row projection for the staff-facing requests table on
 * {@code /admin/requests.xhtml}. Constructor signature matches the JPQL
 * projection on
 * {@link com.oppshan.securedoc.repository.RequestRepository#listForOrganization}
 * so the template's LONGBLOB never enters the SELECT.
 *
 * <p>{@code docType} is exposed alongside {@code documentName} so the
 * column can display the human-friendly template name while the dropdown
 * filter binds to the stable enum. {@code createdAt} is an {@link Instant}
 * to match the auditable-entity column; the {@link #getCreatedAtDisplay()}
 * helper renders it in the system zone for the column body and filter
 * (Faces' {@code <f:convertDateTime>} has no built-in {@code Instant} type).
 */
public class RequestAdminView implements Serializable {

    @Serial
    private static final long serialVersionUID = 7194827361029384721L;

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private UUID id;
    private String referenceNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String documentName;
    private DocumentTemplate.DocType docType;
    private Request.Status status;
    private Instant createdAt;

    public RequestAdminView() {
    }

    public RequestAdminView(UUID id,
                            String referenceNumber,
                            String firstName,
                            String middleName,
                            String lastName,
                            String documentName,
                            DocumentTemplate.DocType docType,
                            Request.Status status,
                            Instant createdAt) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.documentName = documentName;
        this.docType = docType;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getFullName() {
        final var sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName.trim());
        }
        if (middleName != null && !middleName.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(lastName.trim());
        }
        return sb.toString();
    }

    /**
     * Formatted submission timestamp ({@code yyyy-MM-dd HH:mm} in the
     * server's system zone). The data table uses this both for display
     * and for the column's contains-filter, so what staff sees is what
     * they filter on.
     */
    public String getCreatedAtDisplay() {
        return createdAt == null ? "" : DISPLAY_FORMATTER.format(createdAt);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public DocumentTemplate.DocType getDocType() {
        return docType;
    }

    public void setDocType(DocumentTemplate.DocType docType) {
        this.docType = docType;
    }

    public Request.Status getStatus() {
        return status;
    }

    public void setStatus(Request.Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
