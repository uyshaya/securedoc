package com.oppshan.securedoc.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Narrow projection returned by {@code AdminAuthService.createStaff}
 * after a self-service registration succeeds. Confirms the persisted
 * row without exposing fields the registration flow doesn't need
 * (lastLogin, role, etc.). Built by {@code Staff.toRegistrationView()}.
 */
public class StaffRegistrationView implements Serializable {

    @Serial
    private static final long serialVersionUID = 5199120916097796464L;

    private Long id;
    private String fullName;
    private String email;
    private Long barangayId;
    private Boolean isActive;

    public StaffRegistrationView() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getBarangayId() {
        return barangayId;
    }

    public void setBarangayId(Long barangayId) {
        this.barangayId = barangayId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
