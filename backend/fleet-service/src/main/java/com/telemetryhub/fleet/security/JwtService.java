package com.telemetryhub.fleet.security;

import com.telemetryhub.fleet.config.JwtProperties;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JWTVerifier verifier;

    public JwtService(JwtProperties properties) {
        Algorithm algorithm = Algorithm.HMAC256(properties.secret());
        this.verifier = JWT.require(algorithm)
                .withIssuer(properties.issuer())
                .build();
    }

    public DecodedJWT verify(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException ex) {
            throw new InvalidTokenException("Token JWT invalide ou expiré", ex);
        }
    }

    public boolean isRefreshToken(DecodedJWT decoded) {
        return "refresh".equals(decoded.getClaim("type").asString());
    }
}