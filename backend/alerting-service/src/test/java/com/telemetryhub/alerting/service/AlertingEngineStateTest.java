package com.telemetryhub.alerting.service;

import com.telemetryhub.alerting.domain.AlertEvent;
import com.telemetryhub.alerting.domain.AlertRule;
import com.telemetryhub.alerting.domain.AlertSeverity;
import com.telemetryhub.alerting.domain.AlertStatus;
import com.telemetryhub.alerting.domain.ComparisonOperator;
import com.telemetryhub.alerting.persistence.AlertEventRepository;
import com.telemetryhub.alerting.persistence.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AlertingEngineStateTest {

    private final AlertRuleRepository ruleRepo = mock(AlertRuleRepository.class);
    private final AlertEventRepository eventRepo = mock(AlertEventRepository.class);
    private final AlertProducer producer = mock(AlertProducer.class);
    private final AlertingEngine engine = new AlertingEngine(ruleRepo, eventRepo, producer);

    private UUID tenantId;
    private UUID alertId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        alertId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Acknowledge transitions alert to ACKNOWLEDGED")
    void acknowledgeTransitionsAlertToAcknowledged() {
        AlertEvent alert = makeAlert(AlertStatus.OPEN);
        when(eventRepo.findByIdAndTenantId(alertId, tenantId)).thenReturn(Optional.of(alert));
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        engine.acknowledge(alertId, tenantId, userId);

        assertEquals(AlertStatus.ACKNOWLEDGED, alert.getStatus());
        assertEquals(userId, alert.getAcknowledgedBy());
        verify(producer).publish(alert);
    }

    @Test
    @DisplayName("Resolve requires alert to be acknowledged first")
    void resolveRequiresAcknowledgedStatus() {
        AlertEvent alert = makeAlert(AlertStatus.OPEN);
        when(eventRepo.findByIdAndTenantId(alertId, tenantId)).thenReturn(Optional.of(alert));

        assertThrows(IllegalStateException.class, () -> engine.resolve(alertId, tenantId));
    }

    @Test
    @DisplayName("Resolve transitions acknowledged alert to RESOLVED")
    void resolveTransitionsAlertToResolved() {
        AlertEvent alert = makeAlert(AlertStatus.ACKNOWLEDGED);
        when(eventRepo.findByIdAndTenantId(alertId, tenantId)).thenReturn(Optional.of(alert));
        when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        engine.resolve(alertId, tenantId);

        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        verify(producer).publish(alert);
    }

    @Test
    @DisplayName("Update rule enabled=true activates rule")
    void updateRuleEnabledActivatesRule() {
        UUID ruleId = UUID.randomUUID();
        AlertRule rule = new AlertRule(tenantId, "Seuil", "Test", null, "temperature",
                ComparisonOperator.GT, 80.0, "C", AlertSeverity.WARNING);
        rule.setId(ruleId);
        rule.setEnabled(false);
        when(ruleRepo.findByIdAndTenantId(ruleId, tenantId)).thenReturn(Optional.of(rule));
        when(ruleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        engine.updateRuleEnabled(ruleId, tenantId, true);

        assertTrue(rule.isEnabled());
        verify(ruleRepo).save(rule);
    }

    @Test
    @DisplayName("Update rule enabled=false deactivates rule")
    void updateRuleEnabledDeactivatesRule() {
        UUID ruleId = UUID.randomUUID();
        AlertRule rule = new AlertRule(tenantId, "Seuil", "Test", null, "temperature",
                ComparisonOperator.GT, 80.0, "C", AlertSeverity.WARNING);
        rule.setId(ruleId);
        rule.setEnabled(true);
        when(ruleRepo.findByIdAndTenantId(ruleId, tenantId)).thenReturn(Optional.of(rule));
        when(ruleRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        engine.updateRuleEnabled(ruleId, tenantId, false);

        assertFalse(rule.isEnabled());
        verify(ruleRepo).save(rule);
    }

    @Test
    @DisplayName("Acknowledge throws ResourceNotFoundException for unknown alert")
    void acknowledgeThrowsWhenNotFound() {
        when(eventRepo.findByIdAndTenantId(alertId, tenantId)).thenReturn(Optional.empty());

        assertThrows(com.telemetryhub.alerting.service.ResourceNotFoundException.class,
                () -> engine.acknowledge(alertId, tenantId, userId));
    }

    private AlertEvent makeAlert(AlertStatus status) {
        AlertEvent alert = new AlertEvent(
                tenantId, UUID.randomUUID(), "Seuil temperature",
                UUID.randomUUID(), "temperature", 85.0, 80.0,
                ComparisonOperator.GT, AlertSeverity.WARNING,
                "Test message", java.time.Instant.now(), "fp-001");
        alert.setId(alertId);
        alert.setStatus(status);
        return alert;
    }
}