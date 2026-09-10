package com.telemetryhub.alerting.config;

import com.telemetryhub.alerting.domain.AlertRule;
import com.telemetryhub.alerting.domain.AlertSeverity;
import com.telemetryhub.alerting.domain.ComparisonOperator;
import com.telemetryhub.alerting.persistence.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultRulesInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultRulesInitializer.class);

    private final AlertRuleRepository ruleRepository;
    private final List<String> defaultTenantIds;

    public DefaultRulesInitializer(AlertRuleRepository ruleRepository,
                                   @Value("${telemetryhub.alerting.default-rules.tenant-ids:}") String defaultTenantIds) {
        this.ruleRepository = ruleRepository;
        this.defaultTenantIds = defaultTenantIds == null || defaultTenantIds.isBlank()
                ? List.of()
                : Arrays.stream(defaultTenantIds.split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String raw : defaultTenantIds) {
            final UUID tenantId;
            try {
                tenantId = UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                log.warn("Regles par defaut: id de tenant invalide ignore: {}", raw);
                continue;
            }
            if (!ruleRepository.findByTenantId(tenantId).isEmpty()) {
                continue;
            }
            List<AlertRule> defaults = buildDefaults(tenantId);
            ruleRepository.saveAll(defaults);
            log.info("Regles par defaut creees pour le tenant {} ({} regles)", tenantId, defaults.size());
        }
    }

    private List<AlertRule> buildDefaults(UUID tenantId) {
        return List.of(
                new AlertRule(tenantId, "Temperature elevee", "Seuil par defaut: temperature elevee",
                        null, "temperature", ComparisonOperator.GT, 85.0, "C", AlertSeverity.CRITICAL),
                new AlertRule(tenantId, "Vibration excessive", "Seuil par defaut: vibration excessive",
                        null, "vibration", ComparisonOperator.GT, 12.0, "mm/s", AlertSeverity.WARNING),
                new AlertRule(tenantId, "Pression basse", "Seuil par defaut: pression faible",
                        null, "pression", ComparisonOperator.LT, 3.0, "bar", AlertSeverity.WARNING),
                new AlertRule(tenantId, "Debit faible", "Seuil par defaut: debit faible",
                        null, "debit", ComparisonOperator.LT, 40.0, "m3/h", AlertSeverity.WARNING),
                new AlertRule(tenantId, "Humidite anormale", "Seuil par defaut: humidite elevee",
                        null, "humidite", ComparisonOperator.GT, 70.0, "%", AlertSeverity.WARNING));
    }
}