package com.telemetryhub.alerting.service;

import com.telemetryhub.alerting.domain.AlertEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AlertProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AlertTopicsProperties properties;

    public AlertProducer(KafkaTemplate<String, Object> kafkaTemplate,
                         AlertTopicsProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public void publish(AlertEvent alertEvent) {
        kafkaTemplate.send(properties.alertsTopic(), alertEvent.getId().toString(), alertEvent);
    }
}