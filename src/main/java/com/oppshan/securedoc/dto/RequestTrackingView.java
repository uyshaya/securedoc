package com.oppshan.securedoc.dto;

import com.oppshan.securedoc.model.Request;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Narrow projection returned by {@code RequestService.lookupByReference}
 * for the resident-facing status check on the {@code p-track} scene.
 *
 * <p>Carries only the public-facing fields — no requester PII — since
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RequestTrackingView() {
    }

    public RequestTrackingView(String referenceNumber,
                               Request.Status status,
                               String certificateName,
                               String organizationName,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
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
        String[] parts = status.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
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

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
