package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.dto.OrganizationView;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Named
@SessionScoped
public class OrganizationBean implements Serializable {

    @Serial
    private static final long serialVersionUID = -4968282673847231166L;

    private final SystemConfigBean system;
    private final Logger logger;

    private OrganizationView active;

    @Inject
    public OrganizationBean(SystemConfigBean system, Logger logger) {
        this.system = system;
        this.logger = logger;
    }

    protected OrganizationBean() {
        this(null, null);
    }

    public OrganizationView getActive() {
        return active;
    }

    public void setActive(OrganizationView organization) {
        this.active = organization;
    }

    public boolean selectById(UUID id) {
        logger.tracef("Selecting active organization by id %s", id);
        final var found = system.findOrganizationById(id);
        if (found == null) {
            logger.debugf("Could not select organization %s -- not found", id);
            return false;
        }

        this.active = found;
        logger.debugf("Selected organization %s (%s) as active", found.getId(), found.getCode());
        return true;
    }

    public void clear() {
        logger.tracef("Clearing active organization (was %s)", active == null ? null : active.getId());
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
