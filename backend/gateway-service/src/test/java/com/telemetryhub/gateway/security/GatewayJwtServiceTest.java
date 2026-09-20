package com.telemetryhub.gateway.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.telemetryhub.gateway.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GatewayJwtServiceTest {

    private static final String SECRET =
            "TEST_SECRET_KEY_THAT_IS_AT_LEAST_32_BYTES_LONG_FOR_GATEWAY_TESTS_012345";
    private static final String ISSUER = "telemetryhub-auth";
    private GatewayJwtService service;

    @BeforeEach
    void setUp() {
        service = new GatewayJwtService(new JwtProperties(ISSUER, SECRET));
    }

    @Test
    @DisplayName("Verify un token valide et extrait les claims")
    void verifyValidTokenAndExtractClaims() {
        String token = createAccessToken("admin@acme.com", "TENANT_ADMIN", UUID.randomUUID());

        var decoded = service.verify(token);

        assertEquals("admin@acme.com", decoded.getSubject());
        assertEquals("TENANT_ADMIN", decoded.getClaim("role").asString());
        assertFalse(service.isRefreshToken(decoded));
    }

    @Test
    @DisplayName("Detecte un refresh token via le claim type")
    void detectsRefreshToken() {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("admin@acme.com")
                .withClaim("type", "refresh")
                .withClaim("jti", UUID.randomUUID().toString())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(Duration.ofDays(30)))
                .sign(Algorithm.HMAC256(SECRET));

        var decoded = service.verify(token);

        assertTrue(service.isRefreshToken(decoded));
    }

    @Test
    @DisplayName("Rejette un token expire")
    void rejectsExpiredToken() {
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject("admin@acme.com")
                .withIssuedAt(Instant.now().minusSeconds(3600))
                .withExpiresAt(Instant.now().minusSeconds(1800))
                .sign(Algorithm.HMAC256(SECRET));

        assertThrows(InvalidTokenException.class, () -> service.verify(token));
    }

    @Test
    @DisplayName("Rejette un token avec un issuer different")
    void rejectsWrongIssuer() {
        String token = JWT.create()
                .withIssuer("wrong-issuer")
                .withSubject("admin@acme.com")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(900))
                .sign(Algorithm.HMAC256(SECRET));

        assertThrows(InvalidTokenException.class, () -> service.verify(token));
    }

    @Test
    @DisplayName("Rejette un token falsifie")
    void rejectsTamperedToken() {
        String token = createAccessToken("admin@acme.com", "TENANT_ADMIN", UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 3) + "XXX";

        assertThrows(InvalidTokenException.class, () -> service.verify(tampered));
    }

    private String createAccessToken(String subject, String role, UUID tid) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(subject)
                .withClaim("role", role)
                .withClaim("tid", tid.toString())
                .withClaim("uid", UUID.randomUUID().toString())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .sign(Algorithm.HMAC256(SECRET));
    }
}