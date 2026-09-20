package com.telemetryhub.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemetryhub.jwt")
public record JwtProperties(
        String issuer,
        String secret
) {
}