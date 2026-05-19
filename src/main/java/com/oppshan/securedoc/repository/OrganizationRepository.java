package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.common.StatefulWriteRepository;
import com.oppshan.securedoc.model.Organization;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationRepository
        extends CrudRepository<Organization, UUID>, StatefulWriteRepository<Organization> {

    // Hits idx_organization_type_active_name (type, active, name) -- covers
    // WHERE + ORDER BY without a filesort.
    @Query("""
            FROM Organization
            WHERE type = :type AND active = TRUE
            ORDER BY name
            """)
    List<Organization> listByTypeActive(Organization.Type type);

    /**
     * Server-side search for the registration-form autocomplete. Matches the
     * given query (case-insensitive substring) against either {@code name} or
     * {@code code}, scoped to the active organization type. JSF caller is
     * expected to gate this with {@code minQueryLength="2"} so a leading
     * wildcard against the whole table never fires on an empty input.
     *
     * <p>The leading-wildcard LIKE can't itself use an index, but
     * idx_organization_type_active_name narrows the candidate set to active
     * rows of the requested type before the LIKE filter runs.
     */
    @Query("""
            FROM Organization
            WHERE type = :type AND active = TRUE
              AND (LOWER(name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(code) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY name
            """)
    List<Organization> searchByTypeAndQuery(Organization.Type type, String query);
}
