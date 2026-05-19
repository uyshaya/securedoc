package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Organization;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * Repository-layer integration test for {@link DocumentTemplateRepository}.
 * The lone JPQL on this repository is {@code listActiveByOrganizationId},
 * which is covered by {@code idx_document_template_organization_active_name}.
 * The tests check three orthogonal scoping concerns: the {@code active}
 * filter, the organization filter, and the {@code ORDER BY name} clause.
 */
@QuarkusTest
class DocumentTemplateRepositoryTest {

    @Inject
    DocumentTemplateRepository documentTemplateRepository;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldListActiveByOrganizationIdReturningOnlyActiveRows() {
        final var organization = seedOrganization("DTA-");
        final var activeTemplate = newTemplate(organization, "Active Template").setActive(true);
        final var inactiveTemplate = newTemplate(organization, "Inactive Template").setActive(false);
        documentTemplateRepository.insertWithSession(activeTemplate);
        documentTemplateRepository.insertWithSession(inactiveTemplate);
        entityManager.flush();

        final var listed = documentTemplateRepository.listActiveByOrganizationId(organization.getId())
                .stream()
                .map(DocumentTemplate::getName)
                .toList();

        assertThat(listed, contains("Active Template"));
    }

    @Test
    @Transactional
    void shouldListActiveByOrganizationIdScopedToOrg() {
        final var orgA = seedOrganization("DTB-");
        final var orgB = seedOrganization("DTC-");
        final var templateInA = newTemplate(orgA, "Template In A").setActive(true);
        final var templateInB = newTemplate(orgB, "Template In B").setActive(true);
        documentTemplateRepository.insertWithSession(templateInA);
        documentTemplateRepository.insertWithSession(templateInB);
        entityManager.flush();

        final var listedForA = documentTemplateRepository.listActiveByOrganizationId(orgA.getId())
                .stream()
                .map(DocumentTemplate::getName)
                .toList();
        final var listedForB = documentTemplateRepository.listActiveByOrganizationId(orgB.getId())
                .stream()
                .map(DocumentTemplate::getName)
                .toList();

        assertThat(listedForA, contains("Template In A"));
        assertThat(listedForB, contains("Template In B"));
    }

    @Test
    @Transactional
    void shouldListActiveByOrganizationIdOrderedByName() {
        final var organization = seedOrganization("DTD-");
        final var charlie = newTemplate(organization, "Charlie Template").setActive(true);
        final var alpha = newTemplate(organization, "Alpha Template").setActive(true);
        final var bravo = newTemplate(organization, "Bravo Template").setActive(true);
        documentTemplateRepository.insertWithSession(charlie);
        documentTemplateRepository.insertWithSession(alpha);
        documentTemplateRepository.insertWithSession(bravo);
        entityManager.flush();

        final var listed = documentTemplateRepository.listActiveByOrganizationId(organization.getId())
                .stream()
                .map(DocumentTemplate::getName)
                .toList();

        assertThat(listed, contains("Alpha Template", "Bravo Template", "Charlie Template"));
    }

    private Organization seedOrganization(String codePrefix) {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("Template Test Barangay " + codePrefix)
                .setCode(codePrefix + System.nanoTime())
                .setActive(true);
        organizationRepository.insertWithSession(organization);
        entityManager.flush();
        return organization;
    }

    private static DocumentTemplate newTemplate(Organization organization, String name) {
        return new DocumentTemplate()
                .setOrganization(organization)
                .setDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE)
                .setName(name)
                .setDescription("Smoke-test template")
                .setTemplateData(("placeholder-" + name).getBytes(StandardCharsets.UTF_8))
                .setMimeType("application/pdf")
                .setActive(true);
    }
}
