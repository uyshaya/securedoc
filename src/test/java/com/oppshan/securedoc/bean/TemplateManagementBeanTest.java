package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.common.I18n;
import com.oppshan.securedoc.dto.DocumentTemplateView;
import com.oppshan.securedoc.exception.BusinessException;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.service.TemplateManagementService;
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
import org.primefaces.model.file.UploadedFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TemplateManagementBeanTest {

    @Mock
    private TemplateManagementService templateService;

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

    @Mock
    private UploadedFile uploadedFile;

    private TemplateManagementBean bean;
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

        bean = new TemplateManagementBean(templateService, organizationBean, i18n, logger);

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
    void shouldUploadTemplateInvokingService() throws Exception {
        bean.setNewName("Barangay Clearance");
        bean.setNewDescription("Standard");
        bean.setNewDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE);
        given(uploadedFile.getSize()).willReturn(1024L);
        given(uploadedFile.getContentType()).willReturn("application/pdf");
        given(uploadedFile.getContent()).willReturn("%PDF".getBytes(StandardCharsets.UTF_8));
        bean.setNewFile(uploadedFile);

        bean.upload();

        then(templateService).should().createTemplate(
                eq(activeOrgId),
                eq("Barangay Clearance"),
                eq("Standard"),
                eq(DocumentTemplate.DocType.BARANGAY_CLEARANCE),
                any(byte[].class),
                eq("application/pdf"));
    }

    @Test
    void shouldRejectUploadWhenNoActiveOrganization() {
        given(organizationBean.getActiveId()).willReturn(null);
        bean.setNewName("X");
        bean.setNewDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE);

        bean.upload();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("template.upload.no.active.organization"));
        then(templateService).should(never()).createTemplate(any(), anyString(), any(),
                any(DocumentTemplate.DocType.class), any(byte[].class), anyString());
    }

    @Test
    void shouldRejectUploadWhenNameMissing() {
        bean.setNewName("  ");
        bean.setNewDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE);

        bean.upload();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("template.upload.name.required"));
    }

    @Test
    void shouldRejectUploadWhenDocTypeMissing() {
        bean.setNewName("Some Template");
        bean.setNewDocType(null);

        bean.upload();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("template.upload.doctype.required"));
    }

    @Test
    void shouldRejectUploadWhenFileMissing() {
        bean.setNewName("Some Template");
        bean.setNewDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE);
        bean.setNewFile(null);

        bean.upload();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("template.upload.file.required"));
    }

    @Test
    void shouldRejectUploadWhenMimeTypeIsNotPdf() throws Exception {
        bean.setNewName("Some Template");
        bean.setNewDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE);
        given(uploadedFile.getSize()).willReturn(1024L);
        given(uploadedFile.getContentType()).willReturn("application/zip");
        given(uploadedFile.getContent()).willReturn(new byte[]{1, 2, 3});
        bean.setNewFile(uploadedFile);

        bean.upload();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("template.upload.pdf.only"));
    }

    @Test
    void shouldDeleteTemplateInvokingService() {
        final var templateId = UUID.randomUUID();
        final var view = new DocumentTemplateView()
                .setId(templateId)
                .setName("Doomed Template")
                .setDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE);

        bean.deleteTemplate(view);

        then(templateService).should().deleteTemplate(activeOrgId, templateId);
    }

    @Test
    void shouldSurfaceBusinessExceptionMessageWhenUploadFails() throws Exception {
        bean.setNewName("Barangay Clearance");
        bean.setNewDocType(DocumentTemplate.DocType.BARANGAY_CLEARANCE);
        given(uploadedFile.getSize()).willReturn(1024L);
        given(uploadedFile.getContentType()).willReturn("application/pdf");
        given(uploadedFile.getContent()).willReturn("%PDF".getBytes(StandardCharsets.UTF_8));
        bean.setNewFile(uploadedFile);
        willThrow(BusinessException.unknownOrganization(activeOrgId))
                .given(templateService).createTemplate(any(), anyString(), any(),
                        any(DocumentTemplate.DocType.class), any(byte[].class), anyString());

        bean.upload();

        assertThat(capturedMessages.getFirst().getSummary(),
                containsString("template.unknown.organization"));
    }
}
