package com.telemetryhub.ingestion.service;

import com.telemetryhub.ingestion.domain.TelemetryEvent;
import com.telemetryhub.ingestion.persistence.TelemetryReadingRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TelemetryConsumerTest {

    private final TelemetryReadingRepository repository = mock(TelemetryReadingRepository.class);
    private final TelemetryConsumer consumer = new TelemetryConsumer(repository);

    @Test
    void persistsValidEvent() {
        TelemetryEvent event = new TelemetryEvent(
                UUID.randomUUID(), UUID.randomUUID(), "temperature", 78.4, "°C",
                Instant.now(), "SIM", "fp-001");

        consumer.onReading(event);

        verify(repository).save(any());
    }

    @Test
    void ignoresNullEvent() {
        consumer.onReading(null);
        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void ignoresEventWithoutEquipment() {
        TelemetryEvent event = new TelemetryEvent(
                null, UUID.randomUUID(), "temperature", 78.4, "°C",
                Instant.now(), "SIM", "fp-001");

        consumer.onReading(event);

        verify(repository, org.mockito.Mockito.never()).save(any());
    }
}