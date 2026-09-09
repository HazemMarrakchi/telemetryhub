package com.telemetryhub.auth.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.telemetryhub.auth.config.JwtProperties;
import com.telemetryhub.auth.domain.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC256(properties.secret());
        this.verifier = JWT.require(algorithm)
                .withIssuer(properties.issuer())
                .build();
    }

    public String issueAccessToken(User user) {
        return JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(user.getEmail())
                .withClaim("uid", user.getId().toString())
                .withClaim("tid", user.getTenant().getId().toString())
                .withClaim("tname", user.getTenant().getSlug())
                .withClaim("role", user.getRole().name())
                .withClaim("name", user.getFullName())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(properties.accessTokenTtl()))
                .sign(algorithm);
    }

    public String issueRefreshToken(UUID userId, UUID tenantId, Instant expiresAt) {
        String jti = UUID.randomUUID().toString();
        return JWT.create()
                .withIssuer(properties.issuer())
                .withSubject(userId.toString())
                .withClaim("tid", tenantId.toString())
                .withClaim("jti", jti)
                .withClaim("type", "refresh")
                .withIssuedAt(Instant.now())
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    public DecodedJWT verify(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException ex) {
            throw new InvalidTokenException("Token JWT invalide ou expiré", ex);
        }
    }

    public String extractJti(DecodedJWT decoded) {
        return decoded.getClaim("jti").asString();
    }

    public boolean isRefreshToken(DecodedJWT decoded) {
        return "refresh".equals(decoded.getClaim("type").asString());
    }

    public String extractSubject(DecodedJWT decoded) {
        return decoded.getSubject();
    }
}