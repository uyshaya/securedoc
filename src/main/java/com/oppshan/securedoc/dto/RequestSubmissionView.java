package com.oppshan.securedoc.dto;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.model.Request;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Narrow projection returned by {@code RequestService.submitRequest} after a
 * resident submission succeeds. Carries just what the confirmation scene on
 * /request/{slug} renders -- the generated UUID reference plus the row id
 * for any future status-tracking lookup. Built by
 * {@link Request#toSubmissionView()}.
 */
public class RequestSubmissionView implements Serializable {

    @Serial
    private static final long serialVersionUID = 7361928374619283746L;

    private UUID id;

    private String referenceNumber;

    public RequestSubmissionView() {
    }

    public UUID getId() {
        return id;
    }

    public RequestSubmissionView setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public RequestSubmissionView setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final RequestSubmissionView that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(referenceNumber, that.referenceNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, referenceNumber);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("referenceNumber", referenceNumber)
                .toString();
    }
}
