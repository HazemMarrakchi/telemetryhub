package com.telemetryhub.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", schema = "auth",
        indexes = {
                @Index(name = "idx_refresh_token_jti", columnList = "jti", unique = true),
                @Index(name = "idx_refresh_token_user_id", columnList = "userId"),
                @Index(name = "idx_refresh_token_expires_at", columnList = "expiresAt")
        })
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String jti;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(nullable = false)
    private String deviceName = "unknown";

    @Column(nullable = false, length = 64)
    private String tokenType = "REFRESH";

    protected RefreshToken() {
    }

    public RefreshToken(String jti, UUID userId, UUID tenantId, Instant expiresAt, String deviceName) {
        this.jti = jti;
        this.userId = userId;
        this.tenantId = tenantId;
        this.expiresAt = expiresAt;
        this.issuedAt = Instant.now();
        this.deviceName = deviceName == null ? "unknown" : deviceName;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public void revoke() {
        this.revoked = true;
    }

    public UUID getId() {
        return id;
    }

    public String getJti() {
        return jti;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getTokenType() {
        return tokenType;
    }
}