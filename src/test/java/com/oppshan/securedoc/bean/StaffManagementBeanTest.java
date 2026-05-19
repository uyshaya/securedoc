package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.StaffView;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.model.Staff;
import com.oppshan.securedoc.service.StaffManagementService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.Flash;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaffManagementBeanTest {

    @Mock
    private StaffManagementService staffService;

    @Mock
    private OrganizationBean organizationBean;

    @Mock
    private I18n i18n;

    @Mock
    private Logger logger;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    @Mock
    private Flash flash;

    private StaffManagementBean bean;
    private MockedStatic<FacesContext> facesContextMock;
    private List<FacesMessage> capturedMessages;
    private UUID activeOrgId;

    @BeforeEach
    void setUp() {
        activeOrgId = UUID.randomUUID();
        given(organizationBean.getActiveId()).willReturn(activeOrgId);
        given(i18n.get(anyString())).willAnswer(invocation -> invocation.getArgument(0));
        given(i18n.get(anyString(), any(Object.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(i18n.get(anyString(), any(Object[].class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        bean = new StaffManagementBean(staffService, organizationBean, i18n, logger);

        capturedMessages = new ArrayList<>();
        willAnswer(invocation -> {
            capturedMessages.add(invocation.getArgument(1));
            return null;
        }).given(facesContext).addMessage(any(), any(FacesMessage.class));

        given(facesContext.getExternalContext()).willReturn(externalContext);
        given(externalContext.getFlash()).willReturn(flash);

        facesContextMock = mockStatic(FacesContext.class);
        facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
    }

    @AfterEach
    void tearDown() {
        facesContextMock.close();
    }

    @Test
    void shouldListStaffForActiveOrganization() {
        final var listed = List.of(
                new StaffView().setId(UUID.randomUUID()).setFirstName("Alice")
                        .setLastName("A").setRole(Staff.Role.STAFF).setActive(true),
                new StaffView().setId(UUID.randomUUID()).setFirstName("Bob")
                        .setLastName("B").setRole(Staff.Role.ADMIN).setActive(true));
        given(staffService.listByOrganization(activeOrgId)).willReturn(listed);

        // PostConstruct doesn't fire on hand-instantiation; invoke init() via reflection
        // so the reload() branch runs and populates staffList.
        invokePostConstruct(bean);

        assertThat(bean.getStaffList(), is(notNullValue()));
        assertThat(bean.getStaffList().size(), is(2));
        then(staffService).should().listByOrganization(activeOrgId);
    }

    @Test
    void shouldToggleStaffActiveStatus() {
        final var staff = new StaffView()
                .setId(UUID.randomUUID())
                .setFirstName("Alice")
                .setLastName("A")
                .setFullName("Alice A")
                .setActive(true);

        bean.toggleActive(staff);

        then(staffService).should().setActive(staff.getId(), false);
        assertThat(staff.isActive(), is(false));
    }

    @Test
    void shouldChangeStaffRole() {
        final var staff = new StaffView()
                .setId(UUID.randomUUID())
                .setFirstName("Alice")
                .setLastName("A")
                .setFullName("Alice A")
                .setRole(Staff.Role.ADMIN);

        bean.changeRole(staff);

        then(staffService).should().changeRole(staff.getId(), Staff.Role.ADMIN);
    }

    @Test
    void shouldDeleteStaff() {
        final var staff = new StaffView()
                .setId(UUID.randomUUID())
                .setFirstName("Alice")
                .setLastName("A")
                .setFullName("Alice A");
        // After delete, reload() is called -- stub the lookup to return an empty list.
        given(staffService.listByOrganization(activeOrgId)).willReturn(List.of());

        bean.deleteStaff(staff);

        then(staffService).should().deleteStaff(staff.getId());
    }

    @Test
    void shouldSurfaceBusinessExceptionMessageWhenDeleteFails() {
        final var staff = new StaffView()
                .setId(UUID.randomUUID())
                .setFirstName("Alice")
                .setLastName("A")
                .setFullName("Alice A");
        willThrow(BusinessException.staffDeleteFailed(
                new RuntimeException("fk"), "Alice A"))
                .given(staffService).deleteStaff(staff.getId());

        bean.deleteStaff(staff);

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("staff.delete.failed"));
    }

    /**
     * Invokes the package-private {@code init()} that {@code @PostConstruct}
     * would call inside a CDI container. Hand-instantiation skips CDI
     * lifecycle, so we drive it explicitly to exercise the reload path.
     */
    private void invokePostConstruct(StaffManagementBean target) {
        try {
            final var initMethod = StaffManagementBean.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
