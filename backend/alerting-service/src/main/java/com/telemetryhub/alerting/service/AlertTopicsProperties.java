package com.telemetryhub.alerting.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemetryhub.kafka")
public record AlertTopicsProperties(
        String rawTopic,
        String alertsTopic
) {
}