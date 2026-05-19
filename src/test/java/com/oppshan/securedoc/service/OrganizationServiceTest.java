package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.repository.OrganizationRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class OrganizationServiceTest {

    @Inject
    OrganizationService organizationService;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldReturnMatchesForNameSubstring() {
        final var suffix = uniqueSuffix();
        final var alpha = seedOrganization(Organization.Type.BARANGAY,
                "Alpha Sunshine Barangay " + suffix, "ASB-" + suffix, true);
        seedOrganization(Organization.Type.BARANGAY,
                "Beta Sunshine Barangay " + suffix, "BSB-" + suffix, true);
        seedOrganization(Organization.Type.BARANGAY,
                "Unrelated Name " + suffix, "UNR-" + suffix, true);

        final var matches = organizationService
                .searchByTypeAndQuery(Organization.Type.BARANGAY, "Sunshine");

        assertThat(matches, hasItem(viewMatching(alpha)));
        assertThat(matches.size() >= 2, is(true));
        for (final var view : matches) {
            assertThat(view.getName().toLowerCase().contains("sunshine"), is(true));
        }
    }

    @Test
    @Transactional
    void shouldReturnMatchesForCodeSubstring() {
        final var suffix = uniqueSuffix();
        final var match = seedOrganization(Organization.Type.BARANGAY,
                "Named " + suffix, "ZZCODE-" + suffix, true);
        seedOrganization(Organization.Type.BARANGAY,
                "Other " + suffix, "OTH-" + suffix, true);

        final var matches = organizationService
                .searchByTypeAndQuery(Organization.Type.BARANGAY, "ZZCODE");

        assertThat(matches, contains(viewMatching(match)));
    }

    @Test
    @Transactional
    void shouldMatchCaseInsensitively() {
        final var suffix = uniqueSuffix();
        final var seeded = seedOrganization(Organization.Type.BARANGAY,
                "MixedCase Barangay " + suffix, "MIX-" + suffix, true);

        final var matches = organizationService
                .searchByTypeAndQuery(Organization.Type.BARANGAY, "mixedcase");

        assertThat(matches, hasItem(viewMatching(seeded)));
    }

    @Test
    @Transactional
    void shouldTrimQueryBeforeSearching() {
        final var suffix = uniqueSuffix();
        final var seeded = seedOrganization(Organization.Type.BARANGAY,
                "Trimmable Barangay " + suffix, "TRM-" + suffix, true);

        final var matches = organizationService
                .searchByTypeAndQuery(Organization.Type.BARANGAY, "   Trimmable   ");

        assertThat(matches, hasItem(viewMatching(seeded)));
    }

    @Test
    @Transactional
    void shouldReturnEmptyListForBlankQuery() {
        // Seed something matchable so the test would otherwise return a non-empty
        // list -- proving the short-circuit is what's emptying the result.
        seedOrganization(Organization.Type.BARANGAY,
                "Blank Probe " + uniqueSuffix(), "BP-" + uniqueSuffix(), true);

        assertThat(organizationService.searchByTypeAndQuery(Organization.Type.BARANGAY, "   "),
                is(empty()));
    }

    @Test
    @Transactional
    void shouldReturnEmptyListForNullQuery() {
        assertThat(organizationService.searchByTypeAndQuery(Organization.Type.BARANGAY, null),
                is(empty()));
    }

    @Test
    @Transactional
    void shouldRejectNullTypeWithConstraintViolation() {
        // @NotNull on the type parameter is enforced by Hibernate Validator at
        // the CDI interception boundary -- the method body never runs.
        assertThrows(ConstraintViolationException.class,
                () -> organizationService.searchByTypeAndQuery(null, "anything"));
    }

    @Test
    @Transactional
    void shouldScopeSearchByTypeAndIgnoreInactive() {
        // Shared token planted in every seed name so the query matches all
        // three candidates -- the service must then filter on type=BARANGAY
        // and active=TRUE to leave just one.
        final var token = "ScopeProbe" + uniqueSuffix();
        final var visibleBarangay = seedOrganization(Organization.Type.BARANGAY,
                token + " Visible", "SVB-" + uniqueSuffix(), true);
        final var hiddenSchool = seedOrganization(Organization.Type.SCHOOL,
                token + " School", "SVS-" + uniqueSuffix(), true);
        final var inactiveBarangay = seedOrganization(Organization.Type.BARANGAY,
                token + " Inactive", "SVI-" + uniqueSuffix(), false);

        final var matches = organizationService
                .searchByTypeAndQuery(Organization.Type.BARANGAY, token);

        assertThat(matches, contains(viewMatching(visibleBarangay)));
        for (final var view : matches) {
            assertThat("School org leaked into BARANGAY search",
                    view.getId(), is(not(hiddenSchool.getId())));
            assertThat("Inactive org leaked into search",
                    view.getId(), is(not(inactiveBarangay.getId())));
        }
    }

    @Test
    @Transactional
    void shouldReturnViewWhenOrganizationExists() {
        final var organization = seedOrganization(Organization.Type.BARANGAY,
                "FindById Probe " + uniqueSuffix(), "FBI-" + uniqueSuffix(), true);

        final var view = organizationService.findById(organization.getId());

        assertThat(view, is(notNullValue()));
        assertThat(view.getId(), is(organization.getId()));
        assertThat(view.getName(), is(organization.getName()));
        assertThat(view.getCode(), is(organization.getCode()));
        assertThat(view.getType(), is(Organization.Type.BARANGAY));
    }

    @Test
    @Transactional
    void shouldReturnNullWhenOrganizationMissing() {
        assertThat(organizationService.findById(UUID.randomUUID()), is(nullValue()));
    }

    @Test
    @Transactional
    void shouldRejectNullIdWithConstraintViolation() {
        // @NotNull on the id parameter is enforced by Hibernate Validator at
        // the CDI interception boundary -- the method body never runs.
        assertThrows(ConstraintViolationException.class,
                () -> organizationService.findById(null));
    }

    private static String uniqueSuffix() {
        return Long.toString(System.nanoTime());
    }

    private Organization seedOrganization(Organization.Type type,
                                          String name,
                                          String code,
                                          boolean active) {
        final var organization = new Organization()
                .setType(type)
                .setName(name)
                .setCode(code)
                .setActive(active);
        organizationRepository.insertWithSession(organization);
        entityManager.flush();
        return organization;
    }

    /**
     * Hamcrest matcher narrowed to id equality -- the OrganizationView
     * equals contract checks more fields than the test cares about for
     * "is this the row I seeded?" assertions.
     */
    private static org.hamcrest.Matcher<OrganizationView> viewMatching(Organization organization) {
        return new org.hamcrest.TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(OrganizationView view) {
                return organization.getId().equals(view.getId());
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("OrganizationView with id ").appendValue(organization.getId());
            }
        };
    }

    private static <T> org.hamcrest.Matcher<T> not(T value) {
        return org.hamcrest.Matchers.not(is(value));
    }
}
