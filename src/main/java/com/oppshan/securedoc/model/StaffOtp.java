package com.oppshan.securedoc.model;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "staff_otps")
public class StaffOtp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public enum Type {LOGIN, PASSWORD_RESET}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Convert(converter = TypeConverter.class)
    @Column(name = "otp_type", columnDefinition = "ENUM('login','password_reset') DEFAULT 'login'")
    private Type otpType = Type.LOGIN;

    @Column(name = "otp_attempts")
    private Integer otpAttempts = 0;

    @Column(name = "is_used")
    private Boolean isUsed = Boolean.FALSE;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public StaffOtp() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public Type getOtpType() {
        return otpType;
    }

    public void setOtpType(Type otpType) {
        this.otpType = otpType;
    }

    public Integer getOtpAttempts() {
        return otpAttempts;
    }

    public void setOtpAttempts(Integer otpAttempts) {
        this.otpAttempts = otpAttempts;
    }

    public Boolean getIsUsed() {
        return isUsed;
    }

    public void setIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return expiresAt == null || LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaffOtp)) return false;
        return Objects.equals(id, ((StaffOtp) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Bridges Java {@link Type} (uppercase) with the lowercase
     * MySQL ENUM('login','password_reset') in staff_otps.otp_type.
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
