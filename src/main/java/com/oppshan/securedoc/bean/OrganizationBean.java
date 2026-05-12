package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.OrganizationView;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;

@Named
@SessionScoped
public class OrganizationBean implements Serializable {

    @Serial
    private static final long serialVersionUID = -4968282673847231166L;

    @Inject
    SystemConfigBean system;

    private OrganizationView active;

    public OrganizationView getActive() {
        return active;
    }

    public void setActive(OrganizationView organization) {
        this.active = organization;
    }

    public boolean selectById(Long id) {
        OrganizationView found = system.findOrganizationById(id);
        if (found == null) {
            return false;
        }
        this.active = found;
        return true;
    }

    public void clear() {
        this.active = null;
    }

    public boolean isSelected() {
        return active != null;
    }

    public Long getActiveId() {
        return active != null ? active.getId() : null;
    }

    public String getActiveName() {
        return active != null ? active.getName() : null;
    }

    public String getActiveCode() {
        return active != null ? active.getCode() : null;
    }

    public String getActiveAddress() {
        return active != null ? active.getAddress() : null;
    }
}
