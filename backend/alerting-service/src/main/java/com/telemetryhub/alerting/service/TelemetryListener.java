package com.telemetryhub.alerting.service;

import com.telemetryhub.alerting.domain.TelemetryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TelemetryListener {

    private static final Logger log = LoggerFactory.getLogger(TelemetryListener.class);

    private final AlertingEngine engine;

    public TelemetryListener(AlertingEngine engine) {
        this.engine = engine;
    }

    @KafkaListener(
            topics = "${telemetryhub.kafka.raw-topic}",
            groupId = "alerting-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void onReading(TelemetryEvent event) {
        try {
            engine.evaluate(event);
        } catch (Exception ex) {
            log.error("Erreur lors de l'évaluation des règles", ex);
        }
    }
}