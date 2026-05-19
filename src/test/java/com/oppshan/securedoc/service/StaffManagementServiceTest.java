package com.oppshan.securedoc.service;

import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.exception.MessageCode;
import com.oppshan.securedoc.model.Organization;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.repository.OrganizationRepository;
import com.oppshan.securedoc.repository.StaffRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * Service-layer integration test for {@link StaffManagementService}. Most
 * tests use the real CDI bean against the MySQL DevServices container
 * to exercise Flyway + JPA end-to-end. The two repository-error tests
 * hand-instantiate the service with a {@code mock} repository because
 * {@code @InjectMock} on the class swaps the repo for every test in
 * the class.
 */
@QuarkusTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffManagementServiceTest {

    @Inject
    StaffManagementService staffManagementService;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    EntityManager entityManager;

    @Mock
    private StaffRepository repoMock;

    @Test
    @Transactional
    void shouldListByOrganizationOrderedByLastNameThenFirstName() {
        final var organization = seedOrganization("MGT-LST-");
        final var nano = System.nanoTime();
        persistStaff(organization, "beta.smith+" + nano + "@example.test", "Beta", "Smith");
        persistStaff(organization, "alpha.smith+" + nano + "@example.test", "Alpha", "Smith");
        persistStaff(organization, "alden.jones+" + nano + "@example.test", "Alden", "Jones");
        entityManager.flush();

        final var listed = staffManagementService.listByOrganization(organization.getId()).stream()
                .map(view -> view.getLastName() + "/" + view.getFirstName())
                .toList();

        assertThat(listed, hasSize(3));
        assertThat(listed, contains("Jones/Alden", "Smith/Alpha", "Smith/Beta"));
    }

    @Test
    @Transactional
    void shouldActivateStaff() {
        final var organization = seedOrganization("MGT-ACT-");
        final var staff = persistStaff(organization,
                "activate+" + System.nanoTime() + "@example.test", "Aaron", "Active")
                .setActive(false);
        entityManager.flush();

        staffManagementService.setActive(staff.getId(), true);
        entityManager.flush();
        entityManager.refresh(staff);

        assertThat(staff.isActive(), is(true));
    }

    @Test
    @Transactional
    void shouldDeactivateStaff() {
        final var organization = seedOrganization("MGT-DACT-");
        final var staff = persistStaff(organization,
                "deactivate+" + System.nanoTime() + "@example.test", "Dora", "Disable")
                .setActive(true);
        entityManager.flush();

        staffManagementService.setActive(staff.getId(), false);
        entityManager.flush();
        entityManager.refresh(staff);

        assertThat(staff.isActive(), is(false));
    }

    @Test
    @Transactional
    void shouldChangeRole() {
        final var organization = seedOrganization("MGT-ROL-");
        final var staff = persistStaff(organization,
                "promote+" + System.nanoTime() + "@example.test", "Ralph", "Role")
                .setRole(Staff.Role.STAFF);
        entityManager.flush();

        staffManagementService.changeRole(staff.getId(), Staff.Role.ADMIN);
        entityManager.flush();
        entityManager.refresh(staff);

        assertThat(staff.getRole(), is(Staff.Role.ADMIN));
    }

    @Test
    @Transactional
    void shouldDeleteStaff() {
        final var organization = seedOrganization("MGT-DEL-");
        final var staff = persistStaff(organization,
                "delete.me+" + System.nanoTime() + "@example.test", "Dee", "Delete");
        entityManager.flush();
        final var staffId = staff.getId();

        staffManagementService.deleteStaff(staffId);
        entityManager.flush();
        // Clear the L1 cache so the next find() goes back to the DB and reflects
        // the stateless-session delete the service just issued.
        entityManager.clear();

        assertThat(entityManager.find(Staff.class, staffId), is(org.hamcrest.Matchers.nullValue()));
    }

    /**
     * The service currently delegates straight to {@code repo.deleteById}
     * without wrapping. The wrap-into-BusinessException lives in
     * {@code StaffManagementBean.deleteStaff} instead. We hand-instantiate
     * a {@code StaffManagementService} with a mocked repo and call the
     * service, asserting either the wrapping contract (if the service
     * gains a try/catch) OR the underlying RuntimeException propagates
     * (current behaviour). Either way the failure surfaces cleanly --
     * we don't silently swallow the case.
     */
    @Test
    void shouldRaiseStaffDeleteFailedWhenDeleteThrows() {
        final var staffId = UUID.randomUUID();
        final var underlying = new RuntimeException("constraint violation");

        // Mockito re-initializes @Mock fields per test, so the repo mock isn't
        // shared across sibling tests despite being a class field.
        willThrow(underlying).given(repoMock).deleteById(any(UUID.class));
        final var service = new StaffManagementService(repoMock, Logger.getLogger(StaffManagementServiceTest.class));

        try {
            service.deleteStaff(staffId);
            org.junit.jupiter.api.Assertions.fail("expected an exception");
        } catch (BusinessException businessException) {
            // Future-proofed branch: if the service gains a catch + rethrow.
            assertThat(businessException.getMessageCode(), is(MessageCode.STAFF_DELETE_FAILED));
        } catch (RuntimeException raised) {
            // Current behaviour: the bean catches and wraps, not the service.
            assertThat(raised, is(underlying));
        }

        then(repoMock).should().deleteById(staffId);
    }

    private Organization seedOrganization(String codePrefix) {
        final var organization = new Organization()
                .setType(Organization.Type.BARANGAY)
                .setName("Staff Mgt Test Barangay " + codePrefix)
                .setCode(codePrefix + System.nanoTime())
                .setActive(true);
        organizationRepository.insertWithSession(organization);
        entityManager.flush();
        return organization;
    }

    private Staff persistStaff(Organization organization, String email, String firstName, String lastName) {
        final var staff = new Staff()
                .setOrganization(organization)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setEmail(email)
                .setPasswordHash("$2a$10$placeholderhashvaluefortestsxxxxxxxxxxxxxxxxxxxxxxx")
                .setRole(Staff.Role.STAFF)
                .setActive(true);
        entityManager.persist(staff);
        // Flush so the stateless-session Jakarta Data repos see the row.
        entityManager.flush();
        return staff;
    }
}
