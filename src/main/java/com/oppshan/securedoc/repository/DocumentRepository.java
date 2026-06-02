package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.common.StatefulWriteRepository;
import com.oppshan.securedoc.model.Document;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository
        extends CrudRepository<Document, UUID>, StatefulWriteRepository<Document> {

    // Tenant-scoped load: a staff member can only see documents for requests
    // in their own organization. Hits idx_document_request_id then the
    // unique constraint on request_id keeps the join collapse cheap.
    @Query("""
            FROM Document d
            WHERE d.request.id = :requestId
              AND d.request.organization.id = :organizationId
            """)
    Optional<Document> findByRequestIdAndOrganizationId(UUID requestId, UUID organizationId);

    // Existence probe for idempotent issuance -- approve clicked twice must
    // not insert a duplicate (the UNIQUE constraint on request_id would also
    // catch it, but checking first avoids surfacing a constraint violation).
    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE d.request.id = :requestId
            """)
    long countByRequestId(UUID requestId);
}
