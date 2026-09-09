package com.telemetryhub.auth.persistence;

import com.telemetryhub.auth.domain.Invitation;
import com.telemetryhub.auth.domain.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByTenantIdAndStatus(UUID tenantId, InvitationStatus status);

    boolean existsByEmailAndTenantIdAndStatus(String email, UUID tenantId, InvitationStatus status);

    void deleteByExpiresAtBeforeAndStatus(Instant before, InvitationStatus status);
}