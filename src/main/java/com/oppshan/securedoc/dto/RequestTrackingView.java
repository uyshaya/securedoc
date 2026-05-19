package com.oppshan.securedoc.dto;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.model.Request;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Narrow projection returned by {@code RequestService.lookupByReference}
 * for the resident-facing status check on the {@code p-track} scene.
 *
 * <p>Carries only the public-facing fields -- no requester PII -- since
 * the lookup is unauthenticated. The constructor matches the JPQL
 * projection on {@link com.oppshan.securedoc.repository.RequestRepository#findTrackingByReferenceNumber}
 * so the issuing template's LONGBLOB is never selected just to render
 * its name.
 */
public class RequestTrackingView implements Serializable {

    @Serial
    private static final long serialVersionUID = 6371928374129384721L;

    private String referenceNumber;

    private Request.Status status;

    private String certificateName;

    private String organizationName;

    private Instant createdAt;

    private Instant updatedAt;

    public RequestTrackingView() {
    }

    public RequestTrackingView(String referenceNumber,
                               Request.Status status,
                               String certificateName,
                               String organizationName,
                               Instant createdAt,
                               Instant updatedAt) {
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.certificateName = certificateName;
        this.organizationName = organizationName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Human-readable rendering of {@link #status}: "Under Review"
     * instead of "UNDER_REVIEW". Used directly by the track scene.
     */
    public String getStatusLabel() {
        if (status == null) {
            return "";
        }

        final var parts = status.name().toLowerCase().split("_");
        final var label = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                label.append(' ');
            }

            label.append(Character.toUpperCase(parts[index].charAt(0)))
                    .append(parts[index].substring(1));
        }

        return label.toString();
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public RequestTrackingView setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
        return this;
    }

    public Request.Status getStatus() {
        return status;
    }

    public RequestTrackingView setStatus(Request.Status status) {
        this.status = status;
        return this;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public RequestTrackingView setCertificateName(String certificateName) {
        this.certificateName = certificateName;
        return this;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public RequestTrackingView setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public RequestTrackingView setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public RequestTrackingView setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final RequestTrackingView that)) {
            return false;
        }

        return Objects.equals(referenceNumber, that.referenceNumber) &&
               status == that.status &&
               Objects.equals(certificateName, that.certificateName) &&
               Objects.equals(organizationName, that.organizationName) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                referenceNumber,
                status,
                certificateName,
                organizationName,
                createdAt,
                updatedAt
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("referenceNumber", referenceNumber)
                .add("status", status)
                .add("certificateName", certificateName)
                .add("organizationName", organizationName)
                .add("createdAt", createdAt)
                .add("updatedAt", updatedAt)
                .toString();
    }
}
