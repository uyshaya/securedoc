package com.oppshan.securedoc.service;

import com.oppshan.securedoc.dto.RequestCreate;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.exception.MessageCode;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.model.ResidentOtp;
import com.oppshan.securedoc.repository.OrganizationRepository;
import com.oppshan.securedoc.repository.ResidentOtpRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

@QuarkusTest
class RequestServiceTest {

    @Inject
    RequestService requestService;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    ResidentOtpRepository residentOtpRepository;

    @Inject
    EntityManager entityManager;

    @InjectMock
    MailService mailService;

    @Test
    @Transactional
    void shouldIssueEmailOtpAndPersistRow() {
        final var email = "issue.otp+" + System.nanoTime() + "@example.test";

        requestService.issueEmailOtp(email);
        entityManager.flush();

        final var fresh = residentOtpRepository.findLatestUnused(email);
        assertThat(fresh.isPresent(), is(true));
        assertThat(fresh.orElseThrow().getOtpCode().length(), is(6));
        assertThat(fresh.orElseThrow().isUsed(), is(false));

        then(mailService).should(atLeastOnce()).sendResidentOtp(anyString(), anyString());
    }

    @Test
    @Transactional
    void shouldVerifyEmailOtpWhenCodeMatchesAndUnused() {
        final var email = "verify.ok+" + System.nanoTime() + "@example.test";
        final var code = "654321";
        final var otp = persistOtp(email, code,
                Instant.now().plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS));
        entityManager.flush();

        final var ok = requestService.verifyEmailOtp(email, code);
        entityManager.flush();
        entityManager.refresh(otp);

        assertThat(ok, is(true));
        assertThat(otp.isUsed(), is(true));
    }

    @Test
    @Transactional
    void shouldNotVerifyEmailOtpWhenCodeIsWrong() {
        final var email = "verify.no+" + System.nanoTime() + "@example.test";
        persistOtp(email, "111111",
                Instant.now().plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS));
        entityManager.flush();

        final var ok = requestService.verifyEmailOtp(email, "999999");

        assertThat(ok, is(false));
    }

    @Test
    @Transactional
    void shouldSubmitRequestPersistingRequesterAndRequest() {
        final var organization = seedOrganization("REQ-SUB-");
        final var template = seedTemplate(organization);
        entityManager.flush();

        final var form = newRequestCreate(organization.getId(), template.getId(),
                "submit+" + System.nanoTime() + "@example.test");

        final var view = requestService.submitRequest(form);
        entityManager.flush();

        assertThat(view.getId(), is(notNullValue()));
        assertThat(view.getReferenceNumber(), is(notNullValue()));
        // The reference is server-generated; UUID.toString() length is 36.
        assertThat(view.getReferenceNumber().length(), is(36));
    }

    @Test
    @Transactional
    void shouldGenerateUniqueReferenceNumberPerSubmission() {
        final var organization = seedOrganization("REQ-UQ-");
        final var template = seedTemplate(organization);
        entityManager.flush();

        final var emailA = "uniq.a+" + System.nanoTime() + "@example.test";
        final var emailB = "uniq.b+" + System.nanoTime() + "@example.test";

        final var viewA = requestService.submitRequest(newRequestCreate(
                organization.getId(), template.getId(), emailA));
        final var viewB = requestService.submitRequest(newRequestCreate(
                organization.getId(), template.getId(), emailB));
        entityManager.flush();

        assertThat(viewA.getReferenceNumber(), is(not(viewB.getReferenceNumber())));
    }

    @Test
    @Transactional
    void shouldRaiseUnknownOrganizationWhenSubmittingWithMissingOrg() {
        final var organization = seedOrganization("REQ-NOORG-");
        final var template = seedTemplate(organization);
        entityManager.flush();

        final var form = newRequestCreate(UUID.randomUUID(), template.getId(),
                "noorg+" + System.nanoTime() + "@example.test");

        final var raised = assertThrows(BusinessException.class,
                () -> requestService.submitRequest(form));

        assertThat(raised.getMessageCode(), is(MessageCode.TEMPLATE_UNKNOWN_ORGANIZATION));
    }

    @Test
    @Transactional
    void shouldRaiseUnknownTemplateWhenSubmittingWithMissingTemplate() {
        final var organization = seedOrganization("REQ-NOTPL-");
        entityManager.flush();

        final var form = newRequestCreate(organization.getId(), UUID.randomUUID(),
                "notpl+" + System.nanoTime() + "@example.test");

        final var raised = assertThrows(BusinessException.class,
                () -> requestService.submitRequest(form));

        // unknownTemplate shares the TEMPLATE_UNKNOWN_ORGANIZATION code today
        // (see BusinessException.unknownTemplate -- callers will split later).
        assertThat(raised.getMessageCode(), is(MessageCode.TEMPLATE_UNKNOWN_ORGANIZATION));
    }

    @Test
    @Transactional
    void shouldLookupByReferenceWhenReferenceExists() {
        final var organization = seedOrganization("REQ-LK-");
        final var template = seedTemplate(organization);
        entityManager.flush();

        final var submitted = requestService.submitRequest(newRequestCreate(
                organization.getId(), template.getId(),
                "lookup+" + System.nanoTime() + "@example.test"));
        entityManager.flush();

        final var hit = requestService.lookupByReference(submitted.getReferenceNumber());

        assertThat(hit.isPresent(), is(true));
        assertThat(hit.orElseThrow().getReferenceNumber(), is(submitted.getReferenceNumber()));
    }

    @Test
    @Transactional
    void shouldReturnEmptyFromLookupByReferenceWhenReferenceMissing() {
        final var miss = requestService.lookupByReference("no-such-reference-" + System.nanoTime());

        assertThat(miss.isEmpty(), is(true));
    }

    @Test
    @Transactional
    void shouldReturnEmptyFromLookupByReferenceWhenReferenceIsBlank() {
        assertThat(requestService.lookupByReference(null).isEmpty(), is(true));
        assertThat(requestService.lookupByReference("").isEmpty(), is(true));
        assertThat(requestService.lookupByReference("   ").isEmpty(), is(true));
    }

    private Organization seedOrganization(String codePrefix) {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("Request Test Barangay " + codePrefix)
                .setCode(codePrefix + System.nanoTime())
                .setActive(true);
        organizationRepository.insertWithSession(organization);
        entityManager.flush();
        return organization;
    }

    private DocumentTemplate seedTemplate(Organization organization) {
        final var template = new DocumentTemplate()
                .setOrganization(organization)
                .setDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE)
                .setName("Barangay Clearance " + System.nanoTime())
                .setTemplateData("%PDF placeholder".getBytes(StandardCharsets.UTF_8))
                .setMimeType("application/pdf")
                .setActive(true);
        entityManager.persist(template);
        entityManager.flush();
        return template;
    }

    private ResidentOtp persistOtp(String email, String code, Instant expiresAt) {
        final var otp = new ResidentOtp()
                .setEmail(email)
                .setOtpCode(code)
                .setExpiresAt(expiresAt);
        entityManager.persist(otp);
        entityManager.flush();
        return otp;
    }

    private static RequestCreate newRequestCreate(UUID organizationId, UUID templateId, String email) {
        return new RequestCreate()
                .setOrganizationId(organizationId)
                .setTemplateId(templateId)
                .setEmail(email)
                .setFirstName("Test")
                .setLastName("Resident")
                .setSex("M")
                .setDateOfBirth(LocalDate.of(1990, 1, 1))
                .setPurpose("employment");
    }
}
