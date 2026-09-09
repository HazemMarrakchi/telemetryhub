package com.telemetryhub.alerting.service;

import com.telemetryhub.alerting.domain.AlertRule;
import com.telemetryhub.alerting.domain.AlertSeverity;
import com.telemetryhub.alerting.domain.ComparisonOperator;
import com.telemetryhub.alerting.domain.TelemetryEvent;
import com.telemetryhub.alerting.persistence.AlertEventRepository;
import com.telemetryhub.alerting.persistence.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertingEngineTest {

    private final AlertRuleRepository ruleRepository = mock(AlertRuleRepository.class);
    private final AlertEventRepository eventRepository = mock(AlertEventRepository.class);
    private final AlertProducer producer = mock(AlertProducer.class);
    private final AlertingEngine engine = new AlertingEngine(ruleRepository, eventRepository, producer);

    private UUID tenantId;
    private UUID equipmentId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();
    }

    private TelemetryEvent event(double value) {
        return new TelemetryEvent(equipmentId, tenantId, "temperature", value, "°C",
                Instant.now(), "SIM", "fp-001");
    }

    @Test
    void triggersAlertWhenThresholdExceeded() {
        AlertRule rule = rule("Seuil température", "temperature", ComparisonOperator.GT, 80.0);
        when(ruleRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        engine.evaluate(event(85.5));

        verify(eventRepository).save(any());
        verify(producer).publish(any());
    }

    @Test
    void doesNotTriggerWhenUnderThreshold() {
        AlertRule rule = rule("Seuil température", "temperature", ComparisonOperator.GT, 80.0);
        when(ruleRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        engine.evaluate(event(75.0));

        verify(eventRepository, never()).save(any());
        verify(producer, never()).publish(any());
    }

    @Test
    void ignoresOtherMetrics() {
        AlertRule rule = rule("Seuil vibration", "vibration", ComparisonOperator.GT, 4.5);
        when(ruleRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        engine.evaluate(event(90.0));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void appliesCooldown() {
        AlertRule rule = rule("Seuil température", "temperature", ComparisonOperator.GT, 80.0);
        rule.setCooldownMinutes(10);
        when(ruleRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        engine.evaluate(event(85.5));
        engine.evaluate(event(86.0));

        verify(eventRepository, times(1)).save(any());
    }

    @Test
    void filtersByEquipment() {
        AlertRule rule = rule("Seuil sur machine", "temperature", ComparisonOperator.GT, 80.0);
        rule.setEquipmentId(UUID.randomUUID());
        when(ruleRepository.findByTenantIdAndEnabledTrue(tenantId)).thenReturn(List.of(rule));

        engine.evaluate(event(90.0));

        verify(eventRepository, never()).save(any());
    }

    private AlertRule rule(String name, String metric, ComparisonOperator op, double threshold) {
        return new AlertRule(tenantId, name, "Test", null, metric, op, threshold,
                "°C", AlertSeverity.WARNING);
    }
}