package com.oppshan.securedoc.service;

import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.exception.MessageCode;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.repository.DocumentTemplateRepository;
import com.oppshan.securedoc.repository.OrganizationRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class TemplateManagementServiceTest {

    @Inject
    TemplateManagementService templateManagementService;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    DocumentTemplateRepository templateRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldListByOrganizationReturningActiveTemplates() {
        final var organization = seedOrganization("TPL-LST-");
        final var alpha = persistTemplate(organization, "Alpha Template", true);
        persistTemplate(organization, "Zeta Template", true);
        persistTemplate(organization, "Inactive Template", false);
        entityManager.flush();

        final var listed = templateManagementService.listByOrganization(organization.getId());

        assertThat(listed, hasSize(2));
        assertThat(listed.stream().map(view -> view.getName()).toList(),
                contains("Alpha Template", "Zeta Template"));
        assertThat(listed.getFirst().getId(), is(alpha.getId()));
    }

    @Test
    @Transactional
    void shouldReturnEmptyListWhenOrganizationIdIsNull() {
        final var listed = templateManagementService.listByOrganization(null);

        assertThat(listed, is(empty()));
    }

    @Test
    @Transactional
    void shouldCreateTemplatePersistingRowWithActiveDefault() {
        final var organization = seedOrganization("TPL-NEW-");
        final var payload = "%PDF-1.4 placeholder".getBytes(StandardCharsets.UTF_8);

        final var view = templateManagementService.createTemplate(
                organization.getId(),
                "Barangay Clearance",
                "Standard clearance",
                DocumentTemplate.DocType.BARANGAY_CLEARANCE,
                payload,
                "application/pdf");

        assertThat(view.getId(), is(notNullValue()));
        assertThat(view.getName(), is("Barangay Clearance"));
        assertThat(view.getDocType(), is(DocumentTemplate.DocType.BARANGAY_CLEARANCE));

        // Use EntityManager rather than the Jakarta Data repo: the repository
        // runs on a StatelessSession and won't see the row this transaction
        // just inserted via insertWithSession until commit.
        entityManager.flush();
        final var reloaded = entityManager.find(DocumentTemplate.class, view.getId());
        assertThat(reloaded, is(notNullValue()));
        assertThat(reloaded.isActive(), is(true));
        assertThat(reloaded.getOrganization().getId(), is(organization.getId()));
        assertThat(reloaded.getMimeType(), is("application/pdf"));
    }

    @Test
    @Transactional
    void shouldRaiseUnknownOrganizationWhenCreatingTemplateWithMissingOrg() {
        final var missingId = UUID.randomUUID();
        final var payload = "%PDF".getBytes(StandardCharsets.UTF_8);

        final var raised = assertThrows(BusinessException.class,
                () -> templateManagementService.createTemplate(
                        missingId,
                        "Anything",
                        null,
                        DocumentTemplate.DocType.BARANGAY_CLEARANCE,
                        payload,
                        "application/pdf"));

        assertThat(raised.getMessageCode(), is(MessageCode.TEMPLATE_UNKNOWN_ORGANIZATION));
    }

    @Test
    @Transactional
    void shouldDeleteTemplateScopedToOrg() {
        final var organization = seedOrganization("TPL-DEL-");
        final var template = persistTemplate(organization, "Doomed", true);
        entityManager.flush();
        final var templateId = template.getId();

        templateManagementService.deleteTemplate(organization.getId(), templateId);
        entityManager.flush();
        // Clear the L1 cache so the next find() goes back to the DB and reflects
        // the stateless-session delete the service just issued.
        entityManager.clear();

        assertThat(entityManager.find(DocumentTemplate.class, templateId), is(nullValue()));
    }

    @Test
    @Transactional
    void shouldSilentlyNoOpDeleteWhenTemplateBelongsToDifferentOrg() {
        final var ownerOrg = seedOrganization("TPL-OWN-");
        final var otherOrg = seedOrganization("TPL-OTH-");
        final var template = persistTemplate(ownerOrg, "Cross-Tenant", true);
        entityManager.flush();
        final var templateId = template.getId();

        templateManagementService.deleteTemplate(otherOrg.getId(), templateId);
        entityManager.flush();

        // The row still exists -- the silent no-op fired.
        assertThat(entityManager.find(DocumentTemplate.class, templateId), is(notNullValue()));
    }

    @Test
    @Transactional
    void shouldLoadForPreviewScopedToOrg() {
        final var organization = seedOrganization("TPL-PRV-");
        final var template = persistTemplate(organization, "Preview Me", true);
        entityManager.flush();

        final var view = templateManagementService.loadForPreview(organization.getId(), template.getId());

        assertThat(view, is(notNullValue()));
        assertThat(view.fileName(), is("Preview Me"));
        assertThat(view.mimeType(), is("application/pdf"));
        assertThat(view.data().length > 0, is(true));
    }

    @Test
    @Transactional
    void shouldReturnNullFromLoadForPreviewWhenOrgMismatches() {
        final var ownerOrg = seedOrganization("TPL-OWN-PRV-");
        final var otherOrg = seedOrganization("TPL-OTH-PRV-");
        final var template = persistTemplate(ownerOrg, "Cross-Tenant Preview", true);
        entityManager.flush();

        final var view = templateManagementService.loadForPreview(otherOrg.getId(), template.getId());

        assertThat(view, is(nullValue()));
    }

    private Organization seedOrganization(String codePrefix) {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("Template Mgt Test Barangay " + codePrefix)
                .setCode(codePrefix + System.nanoTime())
                .setActive(true);
        organizationRepository.insertWithSession(organization);
        entityManager.flush();
        return organization;
    }

    private DocumentTemplate persistTemplate(Organization organization, String name, boolean active) {
        final var template = new DocumentTemplate()
                .setOrganization(organization)
                .setDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE)
                .setName(name)
                .setTemplateData("%PDF placeholder".getBytes(StandardCharsets.UTF_8))
                .setMimeType("application/pdf")
                .setActive(active);
        entityManager.persist(template);
        // Flush so the stateless-session Jakarta Data repos see the row.
        entityManager.flush();
        return template;
    }
}
