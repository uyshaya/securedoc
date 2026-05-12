package com.oppshan.securedoc.dto;

import com.oppshan.securedoc.model.Staff;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View-facing projection of a {@link com.oppshan.securedoc.model.Staff} row.
 * Mutable POJO so JSF {@code <h:selectOneMenu value="#{s.role}">} can
 * write the new role back before the bean listener delegates the change
 * to the service. Entities never cross the service boundary.
 */
public class StaffView implements Serializable {

    @Serial
    private static final long serialVersionUID = 976875839521764727L;

    private Long id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String email;
    private Staff.Role role;
    private Boolean isActive;
    private Long organizationId;
    private LocalDateTime lastLogin;

    public StaffView() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public Staff.Role getRole() {
        return role;
    }

    public void setRole(Staff.Role role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}