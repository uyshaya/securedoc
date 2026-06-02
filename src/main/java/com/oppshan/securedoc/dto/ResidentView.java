package com.oppshan.securedoc.dto;

import jakarta.annotation.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-side projection of a {@code Resident} masterlist row, used by
 * {@code /admin/residents/residents-management.xhtml} and any future
 * lookup callers (e.g. the request submission flow validating that
 * the applicant is on file).
 */
public class ResidentView implements Serializable {

    @Serial
    private static final long serialVersionUID = 4928374619203847562L;

    private UUID id;

    private String firstName;

    @Nullable
    private String middleName;

    private String lastName;

    private String sex;

    private LocalDate dateOfBirth;

    private String address;

    private Instant createdAt;

    public ResidentView() {
    }

    public ResidentView(UUID id,
                        String firstName,
                        @Nullable String middleName,
                        String lastName,
                        String sex,
                        LocalDate dateOfBirth,
                        String address,
                        Instant createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Nullable
    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(@Nullable String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getFullName() {
        if (middleName == null || middleName.isBlank()) {
            return firstName + " " + lastName;
        }

        return firstName + " " + middleName + " " + lastName;
    }
}
