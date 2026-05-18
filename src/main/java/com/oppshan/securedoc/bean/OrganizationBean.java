package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.OrganizationView;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Named
@SessionScoped
public class OrganizationBean implements Serializable {

    @Serial
    private static final long serialVersionUID = -4968282673847231166L;

    private final SystemConfigBean system;

    private OrganizationView active;

    @Inject
    public OrganizationBean(SystemConfigBean system) {
        this.system = system;
    }

    protected OrganizationBean() {
        this(null);
    }

    public OrganizationView getActive() {
        return active;
    }

    public void setActive(OrganizationView organization) {
        this.active = organization;
    }

    public boolean selectById(UUID id) {
        final var found = system.findOrganizationById(id);
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

    public UUID getActiveId() {
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
