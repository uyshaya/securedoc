package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.common.StatefulWriteRepository;
import com.oppshan.securedoc.model.Resident;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResidentRepository
        extends CrudRepository<Resident, UUID>, StatefulWriteRepository<Resident> {

    // Hits idx_resident_organization_lastname_firstname -- covers WHERE
    // + ORDER BY without a filesort.
    @Query("""
            FROM Resident
            WHERE organization.id = :organizationId
            ORDER BY lastName, firstName
            """)
    List<Resident> listByOrganization(UUID organizationId);

    @Query("""
            SELECT COUNT(r) FROM Resident r
            WHERE r.organization.id = :organizationId
            """)
    long countByOrganization(UUID organizationId);

    @Query("""
            DELETE FROM Resident WHERE organization.id = :organizationId
            """)
    void deleteByOrganization(UUID organizationId);

    // Case-insensitive identity match used by the requests detail view to
    // decide whether the applicant is on the masterlist. Returns a list
    // rather than Optional so duplicate masterlist rows can't blow up the
    // request page; the service caller picks the first.
    @Query("""
            FROM Resident
            WHERE organization.id = :organizationId
              AND LOWER(firstName) = LOWER(:firstName)
              AND LOWER(lastName) = LOWER(:lastName)
              AND dateOfBirth = :dateOfBirth
            """)
    List<Resident> findMatching(UUID organizationId,
                                String firstName,
                                String lastName,
                                LocalDate dateOfBirth);
}
