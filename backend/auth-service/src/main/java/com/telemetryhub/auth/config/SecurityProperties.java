package com.telemetryhub.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "telemetryhub.security")
public record SecurityProperties(
        List<String> allowedOrigins
) {
}