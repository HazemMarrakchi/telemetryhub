package com.telemetryhub.fleet.persistence;

import com.telemetryhub.fleet.domain.Equipment;
import com.telemetryhub.fleet.domain.EquipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    @EntityGraph(attributePaths = {"model", "site"})
    Page<Equipment> findByTenantId(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"model", "site"})
    Page<Equipment> findByTenantIdAndStatus(UUID tenantId, EquipmentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"model", "site"})
    Optional<Equipment> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = {"model", "site"})
    List<Equipment> findByTenantIdAndSiteId(UUID tenantId, UUID siteId);

    boolean existsBySerialNumber(String serialNumber);

    @EntityGraph(attributePaths = {"model", "site"})
    List<Equipment> findByTenantIdAndStatusIn(UUID tenantId, List<EquipmentStatus> statuses);

    @EntityGraph(attributePaths = {"model", "site"})
    List<Equipment> findByNextMaintenanceAtBeforeAndTenantId(Instant before, UUID tenantId);
}