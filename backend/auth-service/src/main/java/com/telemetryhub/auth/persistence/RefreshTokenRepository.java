package com.telemetryhub.auth.persistence;

import com.telemetryhub.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByJti(String jti);

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    void deleteByExpiresAtBefore(Instant before);
}