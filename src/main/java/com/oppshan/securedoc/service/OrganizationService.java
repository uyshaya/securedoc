package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.repository.OrganizationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Read-only facade over {@link OrganizationRepository} for view beans —
 * the layer that {@code SystemConfigBean} and similar bean code talks to.
 * Entities never cross this boundary; callers always
 * receive {@link OrganizationView}s.
 */
@ApplicationScoped
public class OrganizationService {

    private final OrganizationRepository organizationRepo;
    private final Logger logger;

    @Inject
    public OrganizationService(OrganizationRepository organizationRepo,
                               Logger logger) {
        this.organizationRepo = organizationRepo;
        this.logger = logger;
    }

    /**
     * Server-side search for the registration autocomplete. Returns up
     * to a few dozen matches scoped to the active organization type.
     * Trims the query and short-circuits on blank input — callers don't
     * need to defend.
     */
    @Transactional
    public List<OrganizationView> searchByTypeAndQuery(@NotNull Organization.Type type, String query) {
        logger.tracef("Searching %s organizations matching '%s'", type, query);
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return organizationRepo.searchByTypeAndQuery(type, query.trim()).stream()
                .map(Organization::toView)
                .toList();
    }

    /**
     * Single-organization lookup by id. Returns {@code null} when the
     * row doesn't exist; null id is rejected by {@code @NotNull} before
     * the body runs.
     */
    @Transactional
    public OrganizationView findById(@NotNull UUID id) {
        logger.tracef("Looking up organization by id %s", id);
        return organizationRepo.findById(id).map(Organization::toView).orElse(null);
    }
}
