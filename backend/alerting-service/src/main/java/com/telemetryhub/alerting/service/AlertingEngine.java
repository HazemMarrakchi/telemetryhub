package com.telemetryhub.alerting.service;

import com.telemetryhub.alerting.domain.AlertEvent;
import com.telemetryhub.alerting.domain.AlertRule;
import com.telemetryhub.alerting.domain.AlertSeverity;
import com.telemetryhub.alerting.persistence.AlertEventRepository;
import com.telemetryhub.alerting.persistence.AlertRuleRepository;
import com.telemetryhub.alerting.domain.TelemetryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertingEngine {

    private static final Logger log = LoggerFactory.getLogger(AlertingEngine.class);

    private final AlertRuleRepository ruleRepository;
    private final AlertEventRepository eventRepository;
    private final AlertProducer producer;
    private final Map<String, Instant> lastTriggered = new ConcurrentHashMap<>();

    public AlertingEngine(AlertRuleRepository ruleRepository,
                          AlertEventRepository eventRepository,
                          AlertProducer producer) {
        this.ruleRepository = ruleRepository;
        this.eventRepository = eventRepository;
        this.producer = producer;
    }

    public void evaluate(TelemetryEvent event) {
        if (event == null || event.tenantId() == null) {
            return;
        }
        List<AlertRule> rules = ruleRepository.findByTenantIdAndEnabledTrue(event.tenantId());
        for (AlertRule rule : rules) {
            if (matches(rule, event)) {
                trigger(rule, event);
            }
        }
    }

    private boolean matches(AlertRule rule, TelemetryEvent event) {
        if (!rule.getMetric().equalsIgnoreCase(event.metric())) {
            return false;
        }
        if (rule.getEquipmentId() != null && !rule.getEquipmentId().equals(event.equipmentId())) {
            return false;
        }
        return rule.getOperator().evaluate(event.value(), rule.getThreshold());
    }

    private void trigger(AlertRule rule, TelemetryEvent event) {
        String fingerprint = rule.getId() + "|" + event.equipmentId();
        Instant now = Instant.now();
        Instant last = lastTriggered.get(fingerprint);
        if (last != null && now.isBefore(last.plusSeconds(rule.getCooldownMinutes() * 60L))) {
            return;
        }
        lastTriggered.put(fingerprint, now);

        String message = String.format(
                "%s %s %.1f %s (seuil %.1f) — %s",
                rule.getMetric(), rule.getOperator().symbol(), event.value(),
                event.unit() == null ? "" : event.unit(),
                rule.getThreshold(), rule.getName());

        AlertEvent alertEvent = new AlertEvent(
                rule.getTenantId(), rule.getId(), rule.getName(), event.equipmentId(),
                rule.getMetric(), event.value(), rule.getThreshold(), rule.getOperator(),
                rule.getSeverity(), message, now, fingerprint);

        eventRepository.save(alertEvent);
        producer.publish(alertEvent);

        log.warn("Alerte [{}] {}: {}", rule.getSeverity(), rule.getName(), message);
    }

    public void acknowledge(UUID alertId, UUID tenantId, UUID userId) {
        AlertEvent alert = eventRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alerte introuvable: " + alertId));
        alert.acknowledge(userId);
        eventRepository.save(alert);
        producer.publish(alert);
    }

    public void resolve(UUID alertId, UUID tenantId) {
        AlertEvent alert = eventRepository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Alerte introuvable: " + alertId));
        if (alert.getStatus() != com.telemetryhub.alerting.domain.AlertStatus.ACKNOWLEDGED) {
            throw new IllegalStateException(
                    "Seule une alerte acquittée peut être résolue");
        }
        alert.setStatus(com.telemetryhub.alerting.domain.AlertStatus.RESOLVED);
        eventRepository.save(alert);
        producer.publish(alert);
    }

    public void updateRuleEnabled(UUID ruleId, UUID tenantId, boolean enabled) {
        AlertRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Règle introuvable: " + ruleId));
        rule.setEnabled(enabled);
        ruleRepository.save(rule);
    }
}