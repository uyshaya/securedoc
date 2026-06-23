package com.oppshan.securedoc.dto;

import com.oppshan.securedoc.model.Request;
import jakarta.annotation.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * Full single-row projection for the staff-facing request detail sidebar
 * on {@code /admin/requests.xhtml}. Pulls every field staff need to make
 * an approve/reject decision: requester PII, ID image bytes, document
 * info, and audit timestamps. The constructor signature matches the
 * JPQL projection on
 * {@link com.oppshan.securedoc.repository.RequestRepository#findDetailByIdAndOrganization}.
 *
 * <p>The {@code idImageData} LONGBLOB is only loaded for this projection
 * -- the list query on the same repo stays narrow.
 */
public class RequestDetailView implements Serializable {

    @Serial
    private static final long serialVersionUID = 4937261845091283746L;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private UUID id;
    private String referenceNumber;
    private Request.Status status;
    @Nullable
    private String purpose;
    @Nullable
    private String otherPurpose;
    @Nullable
    private String requestNote;
    private Instant createdAt;
    private Instant lastModifiedAt;
    private UUID templateId;
    private String documentName;
    private String firstName;
    @Nullable
    private String middleName;
    private String lastName;
    private String email;
    private String sex;
    private LocalDate dateOfBirth;
    @Nullable
    private String contactNumber;
    @Nullable
    private String idType;
    @Nullable
    private byte[] idImageData;

    public RequestDetailView() {
    }

    public RequestDetailView(UUID id,
                             String referenceNumber,
                             Request.Status status,
                             @Nullable String purpose,
                             @Nullable String otherPurpose,
                             @Nullable String requestNote,
                             Instant createdAt,
                             Instant lastModifiedAt,
                             UUID templateId,
                             String documentName,
                             String firstName,
                             @Nullable String middleName,
                             String lastName,
                             String email,
                             String sex,
                             LocalDate dateOfBirth,
                             @Nullable String contactNumber,
                             @Nullable String idType,
                             @Nullable byte[] idImageData) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.purpose = purpose;
        this.otherPurpose = otherPurpose;
        this.requestNote = requestNote;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
        this.templateId = templateId;
        this.documentName = documentName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.contactNumber = contactNumber;
        this.idType = idType;
        this.idImageData = idImageData;
    }

    public String getFullName() {
        final var builder = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            builder.append(firstName.trim());
        }
        if (middleName != null && !middleName.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(middleName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(lastName.trim());
        }
        return builder.toString();
    }

    public String getSexLabel() {
        if (sex == null) {
            return "";
        }
        return switch (sex) {
            case "M" -> "Male";
            case "F" -> "Female";
            default -> sex;
        };
    }

    public String getCreatedAtDisplay() {
        if (createdAt == null) {
            return "";
        }
        return TIMESTAMP_FORMATTER.format(createdAt);
    }

    public String getLastModifiedAtDisplay() {
        if (lastModifiedAt == null) {
            return "";
        }
        return TIMESTAMP_FORMATTER.format(lastModifiedAt);
    }

    public String getDateOfBirthDisplay() {
        if (dateOfBirth == null) {
            return "";
        }
        return DATE_FORMATTER.format(dateOfBirth);
    }

    public boolean isHasIdImage() {
        return idImageData != null && idImageData.length > 0;
    }

    /**
     * MIME type sniffed from the first few bytes of {@link #idImageData}.
     * The upload form on {@code /request/{slug}} accepts JPEG, PNG, and
     * PDF; anything else falls back to {@code application/octet-stream} so
     * the XHTML can render a generic download link instead of a broken
     * {@code <img>}.
     */
    public String getIdImageMimeType() {
        if (!isHasIdImage()) {
            return "";
        }
        if (idImageData.length >= 3
                && (idImageData[0] & 0xFF) == 0xFF
                && (idImageData[1] & 0xFF) == 0xD8
                && (idImageData[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (idImageData.length >= 4
                && (idImageData[0] & 0xFF) == 0x89
                && idImageData[1] == 'P'
                && idImageData[2] == 'N'
                && idImageData[3] == 'G') {
            return "image/png";
        }
        if (idImageData.length >= 4
                && idImageData[0] == '%'
                && idImageData[1] == 'P'
                && idImageData[2] == 'D'
                && idImageData[3] == 'F') {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    public boolean isIdImageDisplayable() {
        final var mimeType = getIdImageMimeType();
        return mimeType.equals("image/jpeg") || mimeType.equals("image/png");
    }

    public boolean isIdImagePdf() {
        return getIdImageMimeType().equals("application/pdf");
    }

    /**
     * The ID image as a base64 data URL, ready to drop into an
     * {@code <img src="...">} or {@code <a href="...">}. Returns an
     * empty string when no image is on file.
     */
    public String getIdImageDataUrl() {
        if (!isHasIdImage()) {
            return "";
        }
        final var encoded = Base64.getEncoder().encodeToString(idImageData);
        return "data:" + getIdImageMimeType() + ";base64," + encoded;
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

    public Request.Status getStatus() {
        return status;
    }

    public void setStatus(Request.Status status) {
        this.status = status;
    }

    /**
     * True while the request is still in a state staff can act on -- i.e.
     * not in a terminal state. The detail sidebar uses this to hide the
     * reject-note input and the approve/reject buttons once the request
     * is COMPLETED or REJECTED, since those decisions are immutable.
     */
    public boolean isActionable() {
        return status != Request.Status.COMPLETED
                && status != Request.Status.REJECTED;
    }

    @Nullable
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(@Nullable String purpose) {
        this.purpose = purpose;
    }

    @Nullable
    public String getOtherPurpose() {
        return otherPurpose;
    }

    public void setOtherPurpose(@Nullable String otherPurpose) {
        this.otherPurpose = otherPurpose;
    }

    @Nullable
    public String getRequestNote() {
        return requestNote;
    }

    public void setRequestNote(@Nullable String requestNote) {
        this.requestNote = requestNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Nullable
    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(@Nullable String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @Nullable
    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(@Nullable String contactNumber) {
        this.contactNumber = contactNumber;
    }

    @Nullable
    public String getIdType() {
        return idType;
    }

    public void setIdType(@Nullable String idType) {
        this.idType = idType;
    }

    @Nullable
    public byte[] getIdImageData() {
        return idImageData;
    }

    public void setIdImageData(@Nullable byte[] idImageData) {
        this.idImageData = idImageData;
    }
}