package com.oppshan.securedoc.model;

import com.oppshan.securedoc.dto.OrganizationView;
import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Tenant root. A single deployment hosts one organization type
 * (barangay, school, city, …) selected via the
 * {@code securedoc.org.active-type} application property; the
 * {@link Type} discriminator on each row keeps the table open
 * to additional types being added without a schema rewrite.
 */
@Entity
@Table(name = "organizations")
public class Organization implements Serializable {

    @Serial
    private static final long serialVersionUID = 2723256079779038097L;

    public enum Type {
        BARANGAY,
        SCHOOL,
        CITY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = TypeConverter.class)
    @Column(nullable = false,
            columnDefinition = "ENUM('barangay','school','city') NOT NULL DEFAULT 'barangay'")
    private Type type = Type.BARANGAY;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    private String email;

    @Column(name = "is_active")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Organization() {
    }

    public Organization(Long id, Type type, String name, String code, String address) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.code = code;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OrganizationView toView() {
        return new OrganizationView(id, type, name, code, address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Organization)) {
            return false;
        }
        return Objects.equals(id, ((Organization) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }

    /**
     * Bridges the Java {@link Type} (uppercase, JVM convention) with the
     * lowercase MySQL ENUM('barangay','school','city') in the
     * organizations.type column. Mirrors {@code Staff.RoleConverter}.
     */
    @Converter
    public static class TypeConverter implements AttributeConverter<Type, String> {

        @Override
        public String convertToDatabaseColumn(Type type) {
            return type == null ? null : type.name().toLowerCase();
        }

        @Override
        public Type convertToEntityAttribute(String s) {
            return s == null ? null : Type.valueOf(s.toUpperCase());
        }
    }
}
