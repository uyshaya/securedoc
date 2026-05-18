package com.oppshan.securedoc.dto;

import com.oppshan.securedoc.model.Request;

import java.io.Serial;
import java.io.Serializable;

/**
 * Narrow projection returned by {@code RequestService.submitRequest}
 * after a resident submission succeeds. Carries just what the
 * confirmation scene on /user/request.xhtml renders — the
 * generated UUID reference plus the row id for any future
 * status-tracking lookup. Built by {@link Request#toSubmissionView()}.
 */
public class RequestSubmissionView implements Serializable {

    @Serial
    private static final long serialVersionUID = 7361928374619283746L;

    private Long id;
    private String referenceNumber;

    public RequestSubmissionView() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
}
