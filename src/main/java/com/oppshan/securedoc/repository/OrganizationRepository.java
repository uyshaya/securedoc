package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Organization;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends BasicRepository<Organization, Long> {

    @Query("FROM Organization WHERE type = ?1 AND isActive = TRUE ORDER BY name")
    List<Organization> listByTypeActive(Organization.Type type);

    /**
     * Server-side search for the registration-form autocomplete. Matches the
     * given query (case-insensitive substring) against either {@code name} or
     * {@code code}, scoped to the active organization type. JSF caller is
     * expected to gate this with {@code minQueryLength="2"} so a leading
     * wildcard against the whole table never fires on an empty input.
     */
    @Query("FROM Organization WHERE type = ?1 AND isActive = TRUE " +
           "AND (LOWER(name) LIKE LOWER(CONCAT('%', ?2, '%')) " +
           "  OR LOWER(code) LIKE LOWER(CONCAT('%', ?2, '%'))) " +
           "ORDER BY name")
    List<Organization> searchByTypeAndQuery(Organization.Type type, String query);
}
