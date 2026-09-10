package com.telemetryhub.maintenance.persistence;

import com.telemetryhub.maintenance.domain.WorkOrder;
import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    Optional<WorkOrder> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            select w from WorkOrder w
            where w.tenantId = :tenantId
              and (:status is null or w.status = :status)
              and (:priority is null or w.priority = :priority)
              and (:equipmentId is null or w.equipmentId = :equipmentId)
            """)
    Page<WorkOrder> search(@Param("tenantId") UUID tenantId,
                           @Param("status") WorkOrderStatus status,
                           @Param("priority") WorkOrderPriority priority,
                           @Param("equipmentId") UUID equipmentId,
                           Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, WorkOrderStatus status);

    long countByTenantIdAndStatusIn(UUID tenantId, List<WorkOrderStatus> statuses);

    long countByTenantIdAndStatusInAndDueAtBefore(UUID tenantId, List<WorkOrderStatus> statuses, Instant before);

    long countByTenantIdAndCompletedAtAfter(UUID tenantId, Instant after);

    long countByTenantIdAndCompletedAtBetween(UUID tenantId, Instant from, Instant to);

    long countByTenantIdAndCreatedAtBetween(UUID tenantId, Instant from, Instant to);

    boolean existsByTenantIdAndAlertIdAndStatusIn(UUID tenantId, UUID alertId, List<WorkOrderStatus> openStatuses);
}