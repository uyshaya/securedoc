package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.common.AuditableEntity;
import com.oppshan.securedoc.common.AuditableEntityEntityListener;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@EntityListeners({
        AuditableEntityEntityListener.class
})
@Table(name = "staff_otp",
        indexes = {
                @Index(
                        name = "idx_staff_otps_staff_id",
                        columnList = "staff_id"
                ),
                @Index(
                        name = "idx_staff_otps_lookup",
                        columnList = "staff_id,otp_type,used,id"
                )
        }
)
public class StaffOtp
        implements AuditableEntity<StaffOtp>, Serializable {

    @Serial
    private static final long serialVersionUID = 4225984107533782641L;

    public enum Type {
        LOGIN,
        PASSWORD_RESET
    }

    @Id
    @Basic(optional = false)
    @Column(name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL")
    @JdbcTypeCode(SqlTypes.CHAR)
    @UuidGenerator(style = Style.VERSION_7)
    @NotNull
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false,
            targetEntity = Staff.class
    )
    @JoinColumn(
            name = "staff_id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36) NOT NULL"
    )
    @NotNull
    private Staff staff;

    @Basic(optional = false)
    @Column(name = "otp_code",
            nullable = false,
            updatable = false,
            length = 6)
    @NotEmpty
    private String otpCode;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(name = "otp_type",
            nullable = false,
            updatable = false,
            columnDefinition = "ENUM('LOGIN','PASSWORD_RESET') NOT NULL DEFAULT 'LOGIN'")
    @NotNull
    private Type otpType = Type.LOGIN;

    @Basic(optional = false)
    @Column(name = "otp_attempts",
            nullable = false)
    @PositiveOrZero
    private int otpAttempts = 0;

    @Basic(optional = false)
    @Column(name = "used",
            nullable = false)
    private boolean used = false;

    @Basic(optional = false)
    @Column(name = "expires_at",
            nullable = false,
            updatable = false)
    @NotNull
    private Instant expiresAt;

    @Basic(optional = false)
    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    @NotNull
    private Instant createdAt;

    @Basic(optional = false)
    @Column(name = "updated_at",
            nullable = false)
    @NotNull
    private Instant lastModifiedAt;

    public StaffOtp() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public StaffOtp setId(UUID id) {
        this.id = id;
        return this;
    }

    public Staff getStaff() {
        return staff;
    }

    public StaffOtp setStaff(Staff staff) {
        this.staff = staff;
        return this;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public StaffOtp setOtpCode(String otpCode) {
        this.otpCode = otpCode;
        return this;
    }

    public Type getOtpType() {
        return otpType;
    }

    public StaffOtp setOtpType(Type otpType) {
        this.otpType = otpType;
        return this;
    }

    public int getOtpAttempts() {
        return otpAttempts;
    }

    public StaffOtp setOtpAttempts(int otpAttempts) {
        this.otpAttempts = otpAttempts;
        return this;
    }

    public boolean isUsed() {
        return used;
    }

    public StaffOtp setUsed(boolean used) {
        this.used = used;
        return this;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public StaffOtp setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public StaffOtp setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public StaffOtp setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    public boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final StaffOtp that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(staff, that.staff) &&
               Objects.equals(otpCode, that.otpCode) &&
               otpType == that.otpType &&
               otpAttempts == that.otpAttempts &&
               used == that.used &&
               Objects.equals(expiresAt, that.expiresAt) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastModifiedAt, that.lastModifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                staff,
                otpCode,
                otpType,
                otpAttempts,
                used,
                expiresAt,
                createdAt,
                lastModifiedAt
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("staff", staff)
                .add("otpType", otpType)
                .add("otpAttempts", otpAttempts)
                .add("used", used)
                .add("expiresAt", expiresAt)
                .add("createdAt", createdAt)
                .add("lastModifiedAt", lastModifiedAt)
                .toString();
    }
}
