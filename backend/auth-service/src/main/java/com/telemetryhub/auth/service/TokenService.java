package com.telemetryhub.auth.service;

import com.telemetryhub.auth.config.JwtProperties;
import com.telemetryhub.auth.domain.RefreshToken;
import com.telemetryhub.auth.persistence.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public TokenService(RefreshTokenRepository refreshTokenRepository,
                        JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public RefreshToken storeRefreshToken(String jti, UUID userId, UUID tenantId, String deviceName) {
        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        RefreshToken token = new RefreshToken(jti, userId, tenantId, expiresAt, deviceName);
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(String jti) {
        RefreshToken existing = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token inconnu"));
        if (existing.isRevoked() || existing.isExpired()) {
            throw new InvalidCredentialsException("Refresh token révoqué ou expiré");
        }
        existing.revoke();
        return existing;
    }

    @Transactional(readOnly = true)
    public RefreshToken findValidByJti(String jti) {
        RefreshToken token = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token inconnu"));
        if (token.isRevoked() || token.isExpired()) {
            throw new InvalidCredentialsException("Refresh token révoqué ou expiré");
        }
        return token;
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(RefreshToken::revoke);
    }

    @Transactional
    public void revokeRefreshToken(String jti) {
        refreshTokenRepository.findByJti(jti).ifPresent(RefreshToken::revoke);
    }
}