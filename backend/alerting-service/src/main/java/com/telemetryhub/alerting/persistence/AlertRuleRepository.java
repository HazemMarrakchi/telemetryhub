package com.telemetryhub.alerting.persistence;

import com.telemetryhub.alerting.domain.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRuleRepository extends JpaRepository<AlertRule, UUID> {

    List<AlertRule> findByTenantId(UUID tenantId);

    List<AlertRule> findByTenantIdAndEnabledTrue(UUID tenantId);

    Optional<AlertRule> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}