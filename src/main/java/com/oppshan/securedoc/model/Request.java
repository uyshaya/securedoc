package com.oppshan.securedoc.model;

import com.oppshan.securedoc.dto.RequestSubmissionView;
import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A resident's document request. Created with status PENDING by
 * the resident-facing flow on /user/request.xhtml; flows through
 * staff review states ({@link Status#UNDER_REVIEW},
 * {@link Status#PROCESSING}) before either COMPLETED (a {@code
 * documents} row exists) or REJECTED.
 *
 * <p>The {@code reference_number} field carries the resident-facing
 * tracking ID — a {@code UUID.randomUUID().toString()} value
 * generated server-side, surfaced on the confirmation scene and
 * later used by the track-by-reference lookup.
 */
@Entity
@Table(name = "requests")
public class Request implements Serializable {

    @Serial
    private static final long serialVersionUID = 7263918473625019283L;

    public enum Status {
        PENDING,
        UNDER_REVIEW,
        PROCESSING,
        COMPLETED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "reference_number", nullable = false, unique = true, length = 36)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Requester requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private DocumentTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private Staff processedBy;

    @Convert(converter = StatusConverter.class)
    @Column(columnDefinition = "ENUM('pending','under_review','processing','completed','rejected') DEFAULT 'pending'")
    private Status status = Status.PENDING;

    @Column(length = 255)
    private String purpose;

    @Column(name = "other_purpose", columnDefinition = "TEXT")
    private String otherPurpose;

    @Column(name = "request_note", columnDefinition = "TEXT")
    private String requestNote;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Request() {
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

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Requester getRequester() {
        return requester;
    }

    public void setRequester(Requester requester) {
        this.requester = requester;
    }

    public DocumentTemplate getTemplate() {
        return template;
    }

    public void setTemplate(DocumentTemplate template) {
        this.template = template;
    }

    public Staff getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(Staff processedBy) {
        this.processedBy = processedBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getOtherPurpose() {
        return otherPurpose;
    }

    public void setOtherPurpose(String otherPurpose) {
        this.otherPurpose = otherPurpose;
    }

    public String getRequestNote() {
        return requestNote;
    }

    public void setRequestNote(String requestNote) {
        this.requestNote = requestNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public RequestSubmissionView toSubmissionView() {
        RequestSubmissionView view = new RequestSubmissionView();
        view.setId(id);
        view.setReferenceNumber(referenceNumber);
        return view;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Request)) {
            return false;
        }
        return Objects.equals(id, ((Request) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Bridges Java {@link Status} (uppercase) with the lowercase
     * MySQL ENUM in requests.status. Mirrors {@code Staff.RoleConverter}.
     */
    @Converter
    public static class StatusConverter implements AttributeConverter<Status, String> {

        @Override
        public String convertToDatabaseColumn(Status status) {
            return status == null ? null : status.name().toLowerCase();
        }

        @Override
        public Status convertToEntityAttribute(String s) {
            return s == null ? null : Status.valueOf(s.toUpperCase());
        }
    }
}
