package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.DocumentTemplateView;
import com.oppshan.securedoc.dto.OrganizationView;
import com.oppshan.securedoc.model.DocumentTemplate;
import com.oppshan.securedoc.repository.DocumentTemplateRepository;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Backs the multi-scene resident document-request flow on
 * /user/request.xhtml. Session-scoped because the user walks through
 * scenes (landing -> email -> otp -> details -> review -> confirm) via JS
 * scene transitions, and the picked organization/template selections
 * must persist across them. Anonymous flow -- no login required.
 *
 * <p>Currently only the landing scene is JSF-bound; subsequent scenes
 * remain HTML/JS stubs until their backends land.
 */
@Named("requestBean")
@SessionScoped
public class RequestBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 8273419837461829374L;

    private final SystemConfigBean system;
    private final DocumentTemplateRepository templateRepo;

    private OrganizationView selectedOrganization;
    private UUID selectedTemplateId;
    private List<DocumentTemplateView> availableTemplates = List.of();

    @Inject
    public RequestBean(SystemConfigBean system,
                       DocumentTemplateRepository templateRepo) {
        this.system = system;
        this.templateRepo = templateRepo;
    }

    protected RequestBean() {
        this(null, null);
    }

    /** Called by the autocomplete's {@code completeMethod} as the resident types. */
    public List<OrganizationView> completeOrganization(String query) {
        return system.searchOrganizations(query);
    }

    /**
     * AJAX listener fired when the resident picks an organization. Loads
     * that org's active templates so the cert-type dropdown can populate
     * without a full page reload.
     */
    public void onOrganizationSelected() {
        selectedTemplateId = null;

        if (selectedOrganization == null || selectedOrganization.getId() == null) {
            availableTemplates = List.of();
            return;
        }

        availableTemplates = templateRepo.listActiveByOrganizationId(selectedOrganization.getId()).stream()
                .map(DocumentTemplate::toView)
                .toList();
    }

    /**
     * Server-side validation invoked by the landing-scene "Proceed" button.
     * Returns null in all cases -- scene advancement is driven by the
     * button's {@code oncomplete} callback, which checks
     * {@code args.validationFailed} before calling {@code goTo('p-email')}.
     */
    public String proceedFromLanding() {
        final var fc = FacesContext.getCurrentInstance();

        if (selectedOrganization == null || selectedOrganization.getId() == null) {
            fc.addMessage(null, error("Please select your " + system.getOrgLabelLower() + "."));
            fc.validationFailed();
            return null;
        }

        if (selectedTemplateId == null) {
            fc.addMessage(null, error("Please select a certificate type."));
            fc.validationFailed();
            return null;
        }

        return null;
    }

    private static FacesMessage error(String summary) {
        return new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, null);
    }

    public OrganizationView getSelectedOrganization() {
        return selectedOrganization;
    }

    public void setSelectedOrganization(OrganizationView selectedOrganization) {
        this.selectedOrganization = selectedOrganization;
    }

    public UUID getSelectedTemplateId() {
        return selectedTemplateId;
    }

    public void setSelectedTemplateId(UUID selectedTemplateId) {
        this.selectedTemplateId = selectedTemplateId;
    }

    public List<DocumentTemplateView> getAvailableTemplates() {
        return availableTemplates;
    }

    /** Placeholder text for the cert-type dropdown, reflecting the org-selection state. */
    public String getCertTypePlaceholder() {
        if (selectedOrganization == null) {
            return "Select your " + system.getOrgLabelLower() + " first...";
        }

        if (availableTemplates.isEmpty()) {
            return "No certificate types available for this " + system.getOrgLabelLower();
        }

        return "Select a certificate type...";
    }
}
