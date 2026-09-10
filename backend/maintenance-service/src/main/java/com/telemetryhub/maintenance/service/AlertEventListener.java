package com.telemetryhub.maintenance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AlertEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertEventListener.class);

    private final DowntimeService downtimeService;

    public AlertEventListener(DowntimeService downtimeService) {
        this.downtimeService = downtimeService;
    }

    @KafkaListener(
            topics = "${telemetryhub.kafka.alerts-topic}",
            groupId = "maintenance-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void onAlert(AlertMessage alert) {
        try {
            downtimeService.handleAlert(alert);
        } catch (Exception ex) {
            log.error("Erreur lors du traitement de l'alarme {}: {}", alert.id(), ex.getMessage(), ex);
        }
    }
}