package com.telemetryhub.ingestion.api;

import com.telemetryhub.ingestion.persistence.MetricPoint;
import com.telemetryhub.ingestion.service.TelemetryQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryQueryController {

    private final TelemetryQueryService queryService;

    public TelemetryQueryController(TelemetryQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/raw")
    public ResponseEntity<List<MetricPoint>> rawRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID equipmentId) {
        if (equipmentId != null) {
            return ResponseEntity.ok(queryService.rawByEquipment(equipmentId, from, to));
        }
        return ResponseEntity.ok(queryService.raw(from, to));
    }

    @GetMapping("/aggregate")
    public ResponseEntity<List<MetricPoint>> aggregate(
            @RequestParam(required = false, defaultValue = "1 hour") String bucket,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID equipmentId) {
        String normalizedBucket = normalizeBucket(bucket);
        if (equipmentId != null) {
            return ResponseEntity.ok(queryService.aggregateByEquipment(equipmentId, normalizedBucket, from, to));
        }
        return ResponseEntity.ok(queryService.aggregate(normalizedBucket, from, to));
    }

    @GetMapping("/latest")
    public ResponseEntity<List<MetricPoint>> latest() {
        return ResponseEntity.ok(queryService.latest());
    }

    private String normalizeBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            return "1 hour";
        }
        return bucket;
    }
}