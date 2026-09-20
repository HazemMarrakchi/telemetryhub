package com.telemetryhub.auth.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.telemetryhub.auth.config.JwtProperties;
import com.telemetryhub.auth.domain.Role;
import com.telemetryhub.auth.domain.Tenant;
import com.telemetryhub.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "telemetryhub-auth",
                "a-very-long-test-secret-key-0123456789-abcdefghijklmnop",
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                Duration.ofDays(90)
        );
        jwtService = new JwtService(properties);
    }

    private User persistedUser() {
        Tenant tenant = new Tenant("Acme", "acme", "STANDARD");
        ReflectionTestUtils.setField(tenant, "id", UUID.randomUUID());
        User user = new User(tenant, "admin@acme.com", "Admin", "hash", Role.TENANT_ADMIN);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    @Test
    @DisplayName("Emet un access token contenant les claims tenant et role")
    void issuesAccessTokenWithClaims() {
        User user = persistedUser();

        String token = jwtService.issueAccessToken(user);
        assertThat(token).isNotBlank();

        DecodedJWT decoded = jwtService.verify(token);
        assertThat(decoded.getSubject()).isEqualTo("admin@acme.com");
        assertThat(decoded.getClaim("tid").asString()).isEqualTo(user.getTenant().getId().toString());
        assertThat(decoded.getClaim("role").asString()).isEqualTo("TENANT_ADMIN");
        assertThat(jwtService.isRefreshToken(decoded)).isFalse();
    }

    @Test
    @DisplayName("Emet un refresh token marque comme refresh")
    void issuesRefreshToken() {
        String token = jwtService.issueRefreshToken(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().plus(Duration.ofDays(30)));

        DecodedJWT decoded = jwtService.verify(token);
        assertThat(jwtService.isRefreshToken(decoded)).isTrue();
        assertThat(jwtService.extractJti(decoded)).isNotBlank();
    }

    @Test
    @DisplayName("Rejette un token falsifie")
    void rejectsTamperedToken() {
        User user = persistedUser();
        String token = jwtService.issueAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(InvalidTokenException.class, () -> jwtService.verify(tampered));
    }
}