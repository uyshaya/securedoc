package com.oppshan.securedoc.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * Form payload submitted from /admin/register.xhtml. Bundles the
 * fields that {@code AdminAuthService.createStaff} needs so the
 * service signature doesn't grow a long positional argument list.
 * Bean-side fields like {@code confirmPassword} stay in the bean —
 * they never reach the service.
 */
public class StaffRegistrationCreate implements Serializable {

    @Serial
    private static final long serialVersionUID = 8892600475009793842L;

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Long barangayId;

    public StaffRegistrationCreate() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getBarangayId() {
        return barangayId;
    }

    public void setBarangayId(Long barangayId) {
        this.barangayId = barangayId;
    }
}
