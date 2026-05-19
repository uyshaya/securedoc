package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.common.StatefulWriteRepository;
import com.oppshan.securedoc.dto.RequestAdminView;
import com.oppshan.securedoc.dto.RequestTrackingView;
import com.oppshan.securedoc.model.Request;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequestRepository
        extends CrudRepository<Request, UUID>, StatefulWriteRepository<Request> {

    /**
     * Resident status lookup. Projects directly into {@link RequestTrackingView}
     * so the issuing template's LONGBLOB never enters the SELECT list -- we only
     * need its name. The lookup is by the UUID reference exposed to the
     * resident, so no extra scoping is needed. Hits the
     * uc_request_reference_number UNIQUE index.
     */
    @Query("""
            SELECT new com.oppshan.securedoc.dto.RequestTrackingView(
                r.referenceNumber, r.status, t.name, o.name, r.createdAt, r.lastModifiedAt)
            FROM Request r
            JOIN r.template t
            JOIN r.organization o
            WHERE r.referenceNumber = :referenceNumber
            """)
    Optional<RequestTrackingView> findTrackingByReferenceNumber(String referenceNumber);

    /**
     * Admin-table projection for {@code /admin/requests.xhtml}. Joins the
     * requester (for the name column) and template (for the document-type
     * column) but only pulls scalar fields -- the {@code template_data}
     * LONGBLOB stays out of the SELECT list. Scoped to the active
     * organization; in-memory filtering/sorting is handled client-side by
     * the PrimeFaces DataTable. Hits idx_request_organization_id.
     */
    @Query("""
            SELECT new com.oppshan.securedoc.dto.RequestAdminView(
                r.id, r.referenceNumber,
                rq.firstName, rq.middleName, rq.lastName,
                t.name, t.docType,
                r.status, r.createdAt)
            FROM Request r
            JOIN r.requester rq
            JOIN r.template t
            WHERE r.organization.id = :organizationId
            ORDER BY r.createdAt DESC
            """)
    List<RequestAdminView> listForOrganization(UUID organizationId);
}
