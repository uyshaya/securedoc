package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.model.Request;
import com.oppshan.securedoc.model.Requester;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Repository-layer integration test for {@link RequestRepository}. The
 * notable surface is {@code findTrackingByReferenceNumber}, a JPQL
 * constructor projection into {@link com.oppshan.securedoc.dto.RequestTrackingView}.
 * Wiring a request requires three referenced entities: an organization, a
 * document template, and a requester -- {@link #seedRequest(String)} builds
 * the whole graph in one go.
 */
@QuarkusTest
class RequestRepositoryTest {

    @Inject
    RequestRepository requestRepository;

    @Inject
    RequesterRepository requesterRepository;

    @Inject
    DocumentTemplateRepository documentTemplateRepository;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void shouldRoundTripRequestThroughInsertAndFindById() {
        final var request = seedRequest(UUID.randomUUID().toString());

        assertThat("@UuidGenerator should have populated the PK",
                request.getId(), is(notNullValue()));
        assertThat("AuditableEntityEntityListener @PrePersist must set createdAt",
                request.getCreatedAt(), is(notNullValue()));
        assertThat("AuditableEntityEntityListener @PrePersist must set lastModifiedAt",
                request.getLastModifiedAt(), is(notNullValue()));

        final var reloaded = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloaded.getReferenceNumber(), is(request.getReferenceNumber()));
        assertThat(reloaded.getStatus(), is(Request.Status.PENDING));
        assertThat(reloaded.getOrganization().getId(), is(request.getOrganization().getId()));
        assertThat(reloaded.getTemplate().getId(), is(request.getTemplate().getId()));
        assertThat(reloaded.getRequester().getId(), is(request.getRequester().getId()));
    }

    @Test
    @Transactional
    void shouldFindTrackingByReferenceNumberWhenReferenceExists() {
        final var referenceNumber = UUID.randomUUID().toString();
        final var request = seedRequest(referenceNumber);

        final var tracking = requestRepository.findTrackingByReferenceNumber(referenceNumber);

        assertThat(tracking.isPresent(), is(true));
        final var view = tracking.orElseThrow();
        assertThat(view.getReferenceNumber(), is(referenceNumber));
        assertThat(view.getStatus(), is(Request.Status.PENDING));
        assertThat(view.getCertificateName(), is(request.getTemplate().getName()));
        assertThat(view.getOrganizationName(), is(request.getOrganization().getName()));
        assertThat(view.getCreatedAt(), is(notNullValue()));
        assertThat(view.getUpdatedAt(), is(notNullValue()));
    }

    @Test
    @Transactional
    void shouldReturnEmptyFromFindTrackingByReferenceNumberWhenReferenceMissing() {
        final var tracking = requestRepository.findTrackingByReferenceNumber(
                "no-such-reference-" + System.nanoTime());

        assertThat(tracking.isEmpty(), is(true));
    }

    private Request seedRequest(String referenceNumber) {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("Request Test Barangay")
                .setCode("REQ-" + System.nanoTime())
                .setActive(true);
        organizationRepository.insertWithSession(organization);

        final var template = new DocumentTemplate()
                .setOrganization(organization)
                .setDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE)
                .setName("Barangay Clearance Template")
                .setTemplateData("placeholder".getBytes(StandardCharsets.UTF_8))
                .setMimeType("application/pdf")
                .setActive(true);
        documentTemplateRepository.insertWithSession(template);

        final var requester = new Requester()
                .setFirstName("Jane")
                .setLastName("Resident")
                .setEmail("jane.resident+" + System.nanoTime() + "@example.test")
                .setSex("F")
                .setDateOfBirth(LocalDate.of(1985, 6, 1));
        requesterRepository.insertWithSession(requester);

        final var request = new Request()
                .setOrganization(organization)
                .setReferenceNumber(referenceNumber)
                .setRequester(requester)
                .setTemplate(template)
                .setStatus(Request.Status.PENDING)
                .setPurpose("Employment");
        requestRepository.insertWithSession(request);
        entityManager.flush();
        return request;
    }
}
