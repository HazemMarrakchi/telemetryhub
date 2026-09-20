package com.telemetryhub.fleet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "telemetryhub.jwt")
public record JwtProperties(
        String issuer,
        String secret
) {
}