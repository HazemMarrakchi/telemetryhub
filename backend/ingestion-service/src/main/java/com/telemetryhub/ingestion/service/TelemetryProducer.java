package com.telemetryhub.ingestion.service;

import com.telemetryhub.ingestion.domain.TelemetryEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TelemetryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String rawTopic;

    public TelemetryProducer(KafkaTemplate<String, Object> kafkaTemplate,
                             RawTopicProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.rawTopic = properties.name();
    }

    public void publish(TelemetryEvent event) {
        String key = event.equipmentId().toString();
        kafkaTemplate.send(rawTopic, key, event);
    }
}