package com.oppshan.securedoc.bean;

import com.oppshan.securedoc.model.Barangay;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serial;
import java.io.Serializable;

@Named
@SessionScoped
public class BarangayBean implements Serializable {

    @Serial
    private static final long serialVersionUID = -4968282673847231166L;

    @Inject
    SystemConfigBean system;

    private Barangay active;

    public Barangay getActive() {
        return active;
    }

    public void setActive(Barangay barangay) {
        this.active = barangay;
    }

    public boolean selectById(Long id) {
        Barangay found = system.findById(id);
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
