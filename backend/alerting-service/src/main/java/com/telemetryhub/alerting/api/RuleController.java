package com.telemetryhub.alerting.api;

import com.telemetryhub.alerting.domain.AlertRule;
import com.telemetryhub.alerting.persistence.AlertRuleRepository;
import com.telemetryhub.alerting.security.TenantContext;
import com.telemetryhub.alerting.service.AlertingEngine;
import com.telemetryhub.alerting.service.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final AlertRuleRepository ruleRepository;
    private final AlertingEngine engine;

    public RuleController(AlertRuleRepository ruleRepository, AlertingEngine engine) {
        this.ruleRepository = ruleRepository;
        this.engine = engine;
    }

    @GetMapping
    public ResponseEntity<List<RuleView>> list() {
        UUID tenantId = TenantContext.get().getTenantId();
        return ResponseEntity.ok(
                ruleRepository.findByTenantId(tenantId).stream().map(RuleView::from).toList());
    }

    @PostMapping
    public ResponseEntity<RuleView> create(@Valid @RequestBody CreateRuleRequest request) {
        UUID tenantId = TenantContext.get().getTenantId();
        if (ruleRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.name())) {
            throw new IllegalArgumentException("Une règle porte déjà ce nom");
        }
        AlertRule rule = new AlertRule(
                tenantId, request.name(), request.description(), request.equipmentId(),
                request.metric(), request.operator(), request.threshold(),
                request.unit(), request.severity());
        if (request.cooldownMinutes() > 0) {
            rule.setCooldownMinutes(request.cooldownMinutes());
        }
        rule = ruleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(RuleView.from(rule));
    }

    @PatchMapping("/{ruleId}/enabled")
    public ResponseEntity<RuleView> setEnabled(@PathVariable UUID ruleId,
                                               @RequestBody Map<String, Boolean> body) {
        UUID tenantId = TenantContext.get().getTenantId();
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new IllegalArgumentException("Le champ 'enabled' est requis");
        }
        engine.updateRuleEnabled(ruleId, tenantId, enabled);
        AlertRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Règle introuvable: " + ruleId));
        return ResponseEntity.ok(RuleView.from(rule));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID ruleId) {
        UUID tenantId = TenantContext.get().getTenantId();
        AlertRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Règle introuvable: " + ruleId));
        ruleRepository.delete(rule);
        return ResponseEntity.noContent().build();
    }
}