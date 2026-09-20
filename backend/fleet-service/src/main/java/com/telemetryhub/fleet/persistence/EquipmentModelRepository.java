package com.telemetryhub.fleet.persistence;

import com.telemetryhub.fleet.domain.EquipmentModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EquipmentModelRepository extends JpaRepository<EquipmentModel, UUID> {

    Page<EquipmentModel> findByTenantId(UUID tenantId, Pageable pageable);

    List<EquipmentModel> findByTenantIdAndActiveTrue(UUID tenantId);

    Optional<EquipmentModel> findByIdAndTenantId(UUID id, UUID tenantId);
}