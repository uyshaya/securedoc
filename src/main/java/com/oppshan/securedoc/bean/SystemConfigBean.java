package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.repository.OrganizationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@Named
@ApplicationScoped
public class SystemConfigBean {

    public static final String APPLICATION_NAME = "SecureDoc";
    public static final String APPLICATION_VERSION = "1.0.0";

    @Inject
    @ConfigProperty(name = "securedoc.org.active-type", defaultValue = "BARANGAY")
    Organization.Type activeOrgType;

    @Inject
    OrganizationRepository organizationRepo;

    /**
     * Server-side search for the registration autocomplete. Returns up to ~tens
     * of matches scoped to the active organization type. Callers (JSF) should
     * gate with {@code minQueryLength="2"} so a 1-char query doesn't pull half
     * the table.
     */
    public List<OrganizationView> searchOrganizations(String query) {
        if (query == null || query.isBlank()) return List.of();
        return organizationRepo.searchByTypeAndQuery(activeOrgType, query.trim()).stream()
                .map(Organization::toView)
                .toList();
    }

    public OrganizationView findOrganizationById(Long id) {
        if (id == null) return null;
        return organizationRepo.findById(id).map(Organization::toView).orElse(null);
    }

    public Organization.Type getActiveOrgType() {
        return activeOrgType;
    }

    /** Singular, capitalized tenant noun (e.g. "Barangay"). Used for form labels. */
    public String getOrgLabel() {
        return switch (activeOrgType) {
            case BARANGAY -> "Barangay";
            case SCHOOL -> "School";
            case CITY -> "City";
        };
    }

    /** Plural form (e.g. "Barangays"). */
    public String getOrgLabelPlural() {
        return switch (activeOrgType) {
            case BARANGAY -> "Barangays";
            case SCHOOL -> "Schools";
            case CITY -> "Cities";
        };
    }

    /** Lowercase singular for mid-sentence interpolation (e.g. "Please select your barangay."). */
    public String getOrgLabelLower() {
        return getOrgLabel().toLowerCase();
    }

    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    public String getApplicationVersion() {
        return APPLICATION_VERSION;
    }
}
