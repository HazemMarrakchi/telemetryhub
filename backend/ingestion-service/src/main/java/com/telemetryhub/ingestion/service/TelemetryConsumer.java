package com.telemetryhub.ingestion.service;

import com.telemetryhub.ingestion.domain.TelemetryEvent;
import com.telemetryhub.ingestion.domain.TelemetryReading;
import com.telemetryhub.ingestion.persistence.TelemetryReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryConsumer.class);

    private final TelemetryReadingRepository repository;

    public TelemetryConsumer(TelemetryReadingRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(
            topics = "${telemetryhub.kafka.raw-topic}",
            groupId = "ingestion-service",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onReading(TelemetryEvent event) {
        if (event == null || event.equipmentId() == null) {
            log.warn("Event télémétrique mal formé, ignoré");
            return;
        }
        TelemetryReading reading = new TelemetryReading(
                event.equipmentId(), event.tenantId(), event.metric(), event.value(),
                event.unit(), event.recordedAt(), event.source(), event.deviceFingerprint());
        repository.save(reading);
    }
}