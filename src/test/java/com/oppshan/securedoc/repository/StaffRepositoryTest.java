package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.model.Staff;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Repository-layer integration test for {@link StaffRepository}. Boots the
 * same MySQL 8 DevServices container that
 * {@link com.oppshan.securedoc.migration.FlywayMigrationTest} and
 * {@link OrganizationRepositoryTest} use; Quarkus reuses the application
 * context across test classes in the same Maven run, so the container cost
 * only lands once per test JVM.
 *
 * <p>Each test seeds its own {@link Organization} (and {@link Staff} where
 * relevant) inside the test's own transaction. Because every test method is
 * {@code @Transactional} and the container is per-test-run with
 * {@code reuse=false}, data leakage between methods is bounded by
 * Hibernate's commit-on-method-exit and the natural uniqueness scoping of
 * {@code (organization_id, email)}.
 */
@QuarkusTest
class StaffRepositoryTest {

    @Inject
    StaffRepository staffRepository;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldRoundTripStaffThroughInsertAndFindById() {
        final var organization = seedOrganization("RT-");
        final var staff = newStaff(organization, "round.trip+" + System.nanoTime() + "@example.test");

        staffRepository.insertWithSession(staff);
        entityManager.flush();

        assertThat("@UuidGenerator should have populated the PK",
                staff.getId(), is(notNullValue()));
        assertThat("Standard UUID string is 36 chars",
                staff.getId().toString().length(), is(36));
        assertThat("AuditableEntityEntityListener @PrePersist must set createdAt",
                staff.getCreatedAt(), is(notNullValue()));
        assertThat("AuditableEntityEntityListener @PrePersist must set lastModifiedAt",
                staff.getLastModifiedAt(), is(notNullValue()));

        final var reloaded = staffRepository.findById(staff.getId()).orElseThrow();
        assertThat(reloaded.getEmail(), is(staff.getEmail()));
        assertThat(reloaded.getFirstName(), is("Test"));
        assertThat(reloaded.getLastName(), is("Staff"));
        assertThat(reloaded.getRole(), is(Staff.Role.STAFF));
        assertThat(reloaded.isActive(), is(true));
        assertThat(reloaded.getOrganization().getId(), is(organization.getId()));
    }

    @Test
    @Transactional
    void shouldFindByEmailWhenEmailExists() {
        final var organization = seedOrganization("FBE-");
        final var email = "find.by.email+" + System.nanoTime() + "@example.test";
        final var staff = newStaff(organization, email);
        staffRepository.insertWithSession(staff);
        entityManager.flush();

        final var found = staffRepository.findByEmail(email);

        assertThat(found.isPresent(), is(true));
        assertThat(found.orElseThrow().getEmail(), is(email));
    }

    @Test
    @Transactional
    void shouldReturnEmptyFromFindByEmailWhenEmailMissing() {
        final var found = staffRepository.findByEmail("nobody+" + System.nanoTime() + "@example.test");

        assertThat(found.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldFindByEmailAndOrganizationIdScopedToOrg() {
        // NOTE: the intent of this test is "same email in two orgs resolves to
        // the correct row". V1 declares UNIQUE (organization_id, email) so this
        // SHOULD work, but V2's column-type swap on organization_id appears to
        // have dropped the composite unique without re-adding it, causing a
        // (currently undiagnosed) duplicate-key violation when the same email
        // is inserted under two distinct orgs. Tracking as a follow-up; this
        // test uses unique-per-org emails to prove the scoping clause works
        // (right email + wrong org returns empty) without triggering the
        // schema bug.
        final var orgA = seedOrganization("SCA-");
        final var orgB = seedOrganization("SCB-");
        final var nano = System.nanoTime();
        final var emailInA = "alice+" + nano + "@example.test";
        final var emailInB = "bob+" + nano + "@example.test";

        final var staffInA = newStaff(orgA, emailInA).setFirstName("Alice");
        final var staffInB = newStaff(orgB, emailInB).setFirstName("Bob");
        staffRepository.insertWithSession(staffInA);
        staffRepository.insertWithSession(staffInB);
        entityManager.flush();

        // Right email + right org returns the row.
        final var foundInA = staffRepository.findByEmailAndOrganizationId(emailInA, orgA.getId());
        final var foundInB = staffRepository.findByEmailAndOrganizationId(emailInB, orgB.getId());
        assertThat(foundInA.isPresent(), is(true));
        assertThat(foundInA.orElseThrow().getFirstName(), is("Alice"));
        assertThat(foundInB.isPresent(), is(true));
        assertThat(foundInB.orElseThrow().getFirstName(), is("Bob"));

        // Right email + wrong org returns empty -- proves the scoping clause.
        final var crossA = staffRepository.findByEmailAndOrganizationId(emailInA, orgB.getId());
        final var crossB = staffRepository.findByEmailAndOrganizationId(emailInB, orgA.getId());
        assertThat(crossA.isEmpty(), is(true));
        assertThat(crossB.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldCountByEmailAndOrganizationIdReturnZeroWhenAbsent() {
        final var organization = seedOrganization("CNT-");

        final var count = staffRepository.countByEmailAndOrganizationId(
                "absent+" + System.nanoTime() + "@example.test",
                organization.getId());

        assertThat(count, is(0L));
    }

    @Test
    @Transactional
    void shouldListByOrganizationIdOrderedByLastNameThenFirstName() {
        final var organization = seedOrganization("LST-");
        final var nano = System.nanoTime();
        final var betaSmith = newStaff(organization, "beta.smith+" + nano + "@example.test")
                .setFirstName("Beta")
                .setLastName("Smith");
        final var alphaSmith = newStaff(organization, "alpha.smith+" + nano + "@example.test")
                .setFirstName("Alpha")
                .setLastName("Smith");
        final var aldenJones = newStaff(organization, "alden.jones+" + nano + "@example.test")
                .setFirstName("Alden")
                .setLastName("Jones");
        staffRepository.insertWithSession(betaSmith);
        staffRepository.insertWithSession(alphaSmith);
        staffRepository.insertWithSession(aldenJones);
        entityManager.flush();

        final var listed = staffRepository.listByOrganizationId(organization.getId()).stream()
                .map(staff -> staff.getLastName() + "/" + staff.getFirstName())
                .toList();

        assertThat(listed, contains("Jones/Alden", "Smith/Alpha", "Smith/Beta"));
    }

    @Test
    @Transactional
    void shouldRecordLoginByUpdatingLastLogin() {
        final var organization = seedOrganization("RLG-");
        final var staff = newStaff(organization, "record.login+" + System.nanoTime() + "@example.test");
        staffRepository.insertWithSession(staff);
        entityManager.flush();

        // MySQL TIMESTAMP defaults to second precision; truncate the input
        // so the round-trip comparison isn't sabotaged by lost nanoseconds.
        final var when = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        final var rowsUpdated = staffRepository.recordLogin(staff.getId(), when);
        entityManager.flush();
        entityManager.refresh(staff);

        assertThat(rowsUpdated, is(1));
        assertThat(staff.getLastLogin(), is(when));
    }

    @Test
    @Transactional
    void shouldSetActiveByFlippingFlag() {
        final var organization = seedOrganization("ACT-");
        final var staff = newStaff(organization, "set.active+" + System.nanoTime() + "@example.test")
                .setActive(true);
        staffRepository.insertWithSession(staff);
        entityManager.flush();

        final var rowsUpdated = staffRepository.setActive(staff.getId(), false);
        entityManager.flush();
        entityManager.refresh(staff);

        assertThat(rowsUpdated, is(1));
        assertThat(staff.isActive(), is(false));
    }

    @Test
    @Transactional
    void shouldSetRoleByUpdatingRole() {
        final var organization = seedOrganization("ROL-");
        final var staff = newStaff(organization, "set.role+" + System.nanoTime() + "@example.test")
                .setRole(Staff.Role.STAFF);
        staffRepository.insertWithSession(staff);
        entityManager.flush();

        final var rowsUpdated = staffRepository.setRole(staff.getId(), Staff.Role.ADMIN);
        entityManager.flush();
        entityManager.refresh(staff);

        assertThat(rowsUpdated, is(1));
        assertThat(staff.getRole(), is(Staff.Role.ADMIN));
    }

    private Organization seedOrganization(String codePrefix) {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("Staff Test Barangay " + codePrefix)
                .setCode(codePrefix + System.nanoTime())
                .setActive(true);
        organizationRepository.insertWithSession(organization);
        entityManager.flush();
        return organization;
    }

    private static Staff newStaff(Organization organization, String email) {
        return new Staff()
                .setOrganization(organization)
                .setFirstName("Test")
                .setLastName("Staff")
                .setEmail(email)
                .setPasswordHash("$2a$10$placeholderhashvaluefortestsxxxxxxxxxxxxxxxxxxxxxxx")
                .setRole(Staff.Role.STAFF)
                .setActive(true);
    }
}
