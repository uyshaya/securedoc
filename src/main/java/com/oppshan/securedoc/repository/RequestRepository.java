package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.dto.RequestTrackingView;
import com.oppshan.securedoc.model.Request;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.Optional;

@Repository
public interface RequestRepository extends BasicRepository<Request, Long> {

    /**
     * Resident status lookup. Projects directly into {@link RequestTrackingView}
     * so the issuing template's LONGBLOB never enters the SELECT list —
     * we only need its name. The lookup is by the UUID reference exposed
     * to the resident, so no extra scoping is needed.
     */
    @Query("""
            SELECT new com.oppshan.securedoc.dto.RequestTrackingView(
                r.referenceNumber, r.status, t.name, o.name, r.createdAt, r.updatedAt)
            FROM Request r
            JOIN r.template t
            JOIN r.organization o
            WHERE r.referenceNumber = ?1
            """)
    Optional<RequestTrackingView> findTrackingByReferenceNumber(String referenceNumber);
}
