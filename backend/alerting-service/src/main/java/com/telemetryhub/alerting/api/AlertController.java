package com.telemetryhub.alerting.api;

import com.telemetryhub.alerting.domain.AlertSeverity;
import com.telemetryhub.alerting.domain.AlertStatus;
import com.telemetryhub.alerting.domain.AlertEvent;
import com.telemetryhub.alerting.service.AlertingEngine;
import com.telemetryhub.alerting.service.ResourceNotFoundException;
import com.telemetryhub.alerting.persistence.AlertEventRepository;
import com.telemetryhub.alerting.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AlertController {

    private final AlertEventRepository eventRepository;
    private final AlertingEngine engine;

    public AlertController(AlertEventRepository eventRepository,
                           AlertingEngine engine) {
        this.eventRepository = eventRepository;
        this.engine = engine;
    }

    @GetMapping("/alerts")
    public ResponseEntity<Page<AlertView>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) UUID equipmentId) {
        UUID tenantId = TenantContext.get().getTenantId();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "triggeredAt"));
        Page<AlertEvent> result;
        if (status != null) {
            result = eventRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (severity != null) {
            result = eventRepository.findByTenantIdAndSeverity(tenantId, severity, pageable);
        } else if (equipmentId != null) {
            result = eventRepository.findByTenantIdAndEquipmentId(tenantId, equipmentId, pageable);
        } else {
            result = eventRepository.findByTenantId(tenantId, pageable);
        }
        return ResponseEntity.ok(result.map(AlertView::from));
    }

    @PostMapping("/alerts/{alertId}/acknowledge")
    public ResponseEntity<AlertView> acknowledge(@PathVariable UUID alertId) {
        UUID tenantId = TenantContext.get().getTenantId();
        UUID userId = TenantContext.get().getUserId();
        AlertEvent alert = eventRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte introuvable: " + alertId));
        alert.acknowledge(userId);
        alert = eventRepository.save(alert);
        return ResponseEntity.ok(AlertView.from(alert));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<AlertView> resolve(@PathVariable UUID alertId) {
        UUID tenantId = TenantContext.get().getTenantId();
        AlertEvent alert = eventRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte introuvable: " + alertId));
        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new IllegalStateException("Seule une alerte acquittée peut être résolue");
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alert = eventRepository.save(alert);
        return ResponseEntity.ok(AlertView.from(alert));
    }
}