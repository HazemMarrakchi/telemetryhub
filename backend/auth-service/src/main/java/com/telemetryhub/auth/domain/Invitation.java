package com.telemetryhub.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations", schema = "auth",
        indexes = {
                @Index(name = "idx_invitations_token", columnList = "token", unique = true),
                @Index(name = "idx_invitations_tenant_id", columnList = "tenantId"),
                @Index(name = "idx_invitations_email", columnList = "email")
        })
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private UUID invitedBy;

    @Column
    private Instant acceptedAt;

    protected Invitation() {
    }

    public Invitation(String token, UUID tenantId, String email, String fullName,
                      Role role, Instant expiresAt, UUID invitedBy) {
        this.token = token;
        this.tenantId = tenantId;
        this.email = email.toLowerCase();
        this.fullName = fullName;
        this.role = role;
        this.expiresAt = expiresAt;
        this.invitedBy = invitedBy;
        this.createdAt = Instant.now();
        this.status = InvitationStatus.PENDING;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public void markAccepted() {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    public void markRevoked() {
        this.status = InvitationStatus.REVOKED;
    }

    public UUID getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }
}