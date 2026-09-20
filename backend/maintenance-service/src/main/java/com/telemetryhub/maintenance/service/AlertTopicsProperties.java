package com.telemetryhub.maintenance.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemetryhub.kafka")
public record AlertTopicsProperties(String alertsTopic) {
}