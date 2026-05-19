package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.common.AuditableEntity;
import com.oppshan.securedoc.common.AuditableEntityEntityListener;
import com.oppshan.securedoc.dto.RequestSubmissionView;
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
import jakarta.persistence.UniqueConstraint;
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
 * A resident's document request. Created with status PENDING by the
 * resident-facing flow on /user/request.xhtml; flows through staff review
 * states ({@link Status#UNDER_REVIEW}, {@link Status#PROCESSING}) before
 * either COMPLETED (a {@code documents} row exists) or REJECTED.
 *
 * <p>The {@code reference_number} field carries the resident-facing
 * tracking ID -- a {@code UUID.randomUUID().toString()} value generated
 * server-side, surfaced on the confirmation scene and later used by the
 * track-by-reference lookup.
 */
@Entity
@EntityListeners({
        AuditableEntityEntityListener.class
})
@Table(name = "request",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uc_request_reference_number",
                        columnNames = {"reference_number"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_request_organization_id",
                        columnList = "organization_id"
                ),
                @Index(
                        name = "idx_request_requester_id",
                        columnList = "requester_id"
                ),
                @Index(
                        name = "idx_request_template_id",
                        columnList = "template_id"
                ),
                @Index(
                        name = "idx_request_processed_by",
                        columnList = "processed_by"
                ),
                @Index(
                        name = "idx_request_status",
                        columnList = "status"
                )
        }
)
public class Request
        implements AuditableEntity<Request>, Serializable {

    @Serial
    private static final long serialVersionUID = 7263918473625019283L;

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
    @Column(name = "reference_number",
            nullable = false,
            updatable = false,
            length = 36)
    @NotEmpty
    private String referenceNumber;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            targetEntity = Requester.class
    )
    @JoinColumn(
            name = "requester_id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL"
    )
    @NotNull
    private Requester requester;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            targetEntity = DocumentTemplate.class
    )
    @JoinColumn(
            name = "template_id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL"
    )
    @NotNull
    private DocumentTemplate template;

    @ManyToOne(
            fetch = FetchType.LAZY,
            targetEntity = Staff.class
    )
    @JoinColumn(
            name = "processed_by",
            columnDefinition = "CHAR(36)"
    )
    @Nullable
    private Staff processedBy;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(name = "status",
            nullable = false,
            columnDefinition = "ENUM('PENDING','UNDER_REVIEW','PROCESSING','COMPLETED','REJECTED') NOT NULL DEFAULT 'PENDING'")
    @NotNull
    private Status status = Status.PENDING;

    @Column(name = "purpose",
            length = 255)
    @Nullable
    private String purpose;

    @Column(name = "other_purpose",
            columnDefinition = "TEXT")
    @Nullable
    private String otherPurpose;

    @Column(name = "request_note",
            columnDefinition = "TEXT")
    @Nullable
    private String requestNote;

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

    public Request() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Request setId(UUID id) {
        this.id = id;
        return this;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Request setOrganization(Organization organization) {
        this.organization = organization;
        return this;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public Request setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
        return this;
    }

    public Requester getRequester() {
        return requester;
    }

    public Request setRequester(Requester requester) {
        this.requester = requester;
        return this;
    }

    public DocumentTemplate getTemplate() {
        return template;
    }

    public Request setTemplate(DocumentTemplate template) {
        this.template = template;
        return this;
    }

    @Nullable
    public Staff getProcessedBy() {
        return processedBy;
    }

    public Request setProcessedBy(@Nullable Staff processedBy) {
        this.processedBy = processedBy;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public Request setStatus(Status status) {
        this.status = status;
        return this;
    }

    @Nullable
    public String getPurpose() {
        return purpose;
    }

    public Request setPurpose(@Nullable String purpose) {
        this.purpose = purpose;
        return this;
    }

    @Nullable
    public String getOtherPurpose() {
        return otherPurpose;
    }

    public Request setOtherPurpose(@Nullable String otherPurpose) {
        this.otherPurpose = otherPurpose;
        return this;
    }

    @Nullable
    public String getRequestNote() {
        return requestNote;
    }

    public Request setRequestNote(@Nullable String requestNote) {
        this.requestNote = requestNote;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Request setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public Request setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    public RequestSubmissionView toSubmissionView() {
        return new RequestSubmissionView()
                .setId(id)
                .setReferenceNumber(referenceNumber);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final Request that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(organization, that.organization) &&
               Objects.equals(referenceNumber, that.referenceNumber) &&
               Objects.equals(requester, that.requester) &&
               Objects.equals(template, that.template) &&
               Objects.equals(processedBy, that.processedBy) &&
               status == that.status &&
               Objects.equals(purpose, that.purpose) &&
               Objects.equals(otherPurpose, that.otherPurpose) &&
               Objects.equals(requestNote, that.requestNote) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastModifiedAt, that.lastModifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                organization,
                referenceNumber,
                requester,
                template,
                processedBy,
                status,
                purpose,
                otherPurpose,
                requestNote,
                createdAt,
                lastModifiedAt
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("referenceNumber", referenceNumber)
                .add("status", status)
                .add("purpose", purpose)
                .add("createdAt", createdAt)
                .add("lastModifiedAt", lastModifiedAt)
                .toString();
    }

    public enum Status {
        PENDING("Pending"),
        UNDER_REVIEW("Under Review"),
        PROCESSING("Processing"),
        COMPLETED("Completed"),
        REJECTED("Rejected");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
