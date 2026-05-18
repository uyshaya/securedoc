package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Organization;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository-layer integration test for {@link OrganizationRepository}.
 * Spins up against the same MySQL 8 DevServices container that
 * {@link com.oppshan.securedoc.migration.FlywayMigrationTest} uses (Quarkus
 * reuses the application context across test classes in the same Maven
 * run, so this doesn't pay the container-start cost twice).
 *
 * <p>The round-trip is the canonical proof that everything wired up
 * correctly end-to-end:
 * <ul>
 *   <li>{@code @UuidGenerator(style = VERSION_7)} populates the PK with a
 *       v7 UUID at persist time.</li>
 *   <li>{@code @Enumerated(EnumType.STRING)} writes the enum's
 *       {@code name()} value into the MySQL ENUM column.</li>
 *   <li>{@code AuditableEntityEntityListener.@PrePersist} sets
 *       {@code createdAt} and {@code lastModifiedAt}.</li>
 *   <li>{@code StatefulWriteRepository.insertWithSession} merges through
 *       the active JPA session, so the entity is reachable via
 *       {@code findById} in the same transaction.</li>
 * </ul>
 */
@QuarkusTest
class OrganizationRepositoryTest {

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldRoundTripOrganizationThroughInsertAndFindById() {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("Smoke Test Barangay")
                .setCode("SMK-" + System.nanoTime())
                .setActive(true);

        organizationRepository.insertWithSession(organization);
        // Force-flush so the listener-assigned id and audit timestamps land.
        entityManager.flush();

        assertNotNull(organization.getId(), "@UuidGenerator should have populated the PK");
        assertEquals(36, organization.getId().toString().length(),
                "Standard UUID string is 36 chars");
        assertNotNull(organization.getCreatedAt(),
                "AuditableEntityEntityListener @PrePersist must set createdAt");
        assertNotNull(organization.getLastModifiedAt(),
                "AuditableEntityEntityListener @PrePersist must set lastModifiedAt");

        final var reloaded = organizationRepository.findById(organization.getId()).orElseThrow();
        assertEquals("Smoke Test Barangay", reloaded.getName());
        assertEquals(Organization.Type.BARANGAY, reloaded.getType());
        assertTrue(reloaded.isActive());
    }
}
