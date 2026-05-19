package com.oppshan.securedoc.model;

import com.google.common.base.MoreObjects;
import com.oppshan.securedoc.common.AuditableEntity;
import com.oppshan.securedoc.common.AuditableEntityEntityListener;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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

/**
 * One-time verification code for an anonymous resident submitting a document
 * request. Keyed by email (no FK -- residents don't have accounts). Mirrors
 * {@link StaffOtp} structurally minus the staff/type discriminator: residents
 * only have one OTP flow.
 */
@Entity
@EntityListeners({
        AuditableEntityEntityListener.class
})
@Table(name = "resident_otp",
        indexes = {
                @Index(
                        name = "idx_resident_otp_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_resident_otp_expires_at",
                        columnList = "expires_at"
                )
        }
)
public class ResidentOtp
        implements AuditableEntity<ResidentOtp>, Serializable {

    @Serial
    private static final long serialVersionUID = 8217492837461928374L;

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

    @Basic(optional = false)
    @Column(name = "email",
            nullable = false,
            length = 255)
    @NotEmpty
    private String email;

    @Basic(optional = false)
    @Column(name = "otp_code",
            nullable = false,
            updatable = false,
            length = 6)
    @NotEmpty
    private String otpCode;

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

    public ResidentOtp() {
    }

    public boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ResidentOtp setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public ResidentOtp setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public ResidentOtp setOtpCode(String otpCode) {
        this.otpCode = otpCode;
        return this;
    }

    public int getOtpAttempts() {
        return otpAttempts;
    }

    public ResidentOtp setOtpAttempts(int otpAttempts) {
        this.otpAttempts = otpAttempts;
        return this;
    }

    public boolean isUsed() {
        return used;
    }

    public ResidentOtp setUsed(boolean used) {
        this.used = used;
        return this;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ResidentOtp setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public ResidentOtp setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    @Override
    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public ResidentOtp setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
        return this;
    }

    // otpCode excluded from equals / toString -- it's the secret the OTP is meant to protect.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof final ResidentOtp that)) {
            return false;
        }

        return Objects.equals(id, that.id) &&
               Objects.equals(email, that.email) &&
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
                email,
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
                .add("email", email)
                .add("otpAttempts", otpAttempts)
                .add("used", used)
                .add("expiresAt", expiresAt)
                .add("createdAt", createdAt)
                .toString();
    }
}
