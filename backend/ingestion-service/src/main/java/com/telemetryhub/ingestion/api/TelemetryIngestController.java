package com.telemetryhub.ingestion.api;

import com.telemetryhub.ingestion.domain.TelemetryEvent;
import com.telemetryhub.ingestion.service.TelemetryProducer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion")
public class TelemetryIngestController {

    private final TelemetryProducer producer;

    public TelemetryIngestController(TelemetryProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/telemetry")
    public ResponseEntity<IngestReceipt> ingest(
            @Valid @NotNull @RequestBody TelemetryEvent event) {
        producer.publish(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new IngestReceipt(event.equipmentId(), event.metric(),
                        "HORS_QUEUE", "Événement accepté pour ingestion"));
    }

    public record IngestReceipt(
            java.util.UUID equipmentId,
            String metric,
            String status,
            String message
    ) {
    }
}