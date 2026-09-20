package com.telemetryhub.ingestion.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telemetryhub.kafka")
public record RawTopicProperties(String rawTopic) {

    public String name() {
        return rawTopic;
    }
}