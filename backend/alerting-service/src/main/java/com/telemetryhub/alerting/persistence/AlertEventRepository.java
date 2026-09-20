package com.telemetryhub.alerting.persistence;

import com.telemetryhub.alerting.domain.AlertEvent;
import com.telemetryhub.alerting.domain.AlertSeverity;
import com.telemetryhub.alerting.domain.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlertEventRepository extends JpaRepository<AlertEvent, UUID> {

    Page<AlertEvent> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AlertEvent> findByTenantIdAndEquipmentId(UUID tenantId, UUID equipmentId, Pageable pageable);

    Page<AlertEvent> findByTenantIdAndStatus(UUID tenantId, AlertStatus status, Pageable pageable);

    Page<AlertEvent> findByTenantIdAndSeverity(UUID tenantId, AlertSeverity severity, Pageable pageable);

    Optional<AlertEvent> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<AlertEvent> findFirstByTenantIdAndFingerprintOrderByTriggeredAtDesc(UUID tenantId, String fingerprint);
}