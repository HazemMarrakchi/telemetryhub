package com.telemetryhub.maintenance.service;

import com.telemetryhub.maintenance.api.CreateWorkOrderRequest;
import com.telemetryhub.maintenance.api.WorkOrderView;
import com.telemetryhub.maintenance.domain.WorkOrder;
import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderSource;
import com.telemetryhub.maintenance.domain.WorkOrderStatus;
import com.telemetryhub.maintenance.persistence.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkOrderServiceTest {

    private final WorkOrderRepository repository = mock(WorkOrderRepository.class);
    private final WorkOrderService service = new WorkOrderService(repository);

    private UUID tenantId;
    private UUID equipmentId;
    private UUID technicianId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();
        technicianId = UUID.randomUUID();
    }

    private WorkOrder persistedOrder(WorkOrderStatus status) {
        WorkOrder order = new WorkOrder(tenantId, equipmentId, "Changer le roulement", "Opération", 
                WorkOrderPriority.HIGH, WorkOrderSource.MANUAL, null, null);
        order.setStatus(status);
        when(repository.findByIdAndTenantId(eq(order.getId()), eq(tenantId)))
                .thenReturn(Optional.of(order));
        return order;
    }

    @Test
    void createsManualWorkOrder() {
        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                equipmentId, "Changer le roulement", "Remplacement du roulement N°12",
                WorkOrderPriority.HIGH, Instant.now().plusSeconds(3600));
        when(repository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkOrderView view = service.create(tenantId, request);

        assertEquals("Changer le roulement", view.title());
        assertEquals(WorkOrderStatus.CREATED, view.status());
        assertEquals(WorkOrderSource.MANUAL, view.source());
        assertEquals(WorkOrderPriority.HIGH, view.priority());
    }

    @Test
    void assignTransitionsToAssigned() {
        WorkOrder order = persistedOrder(WorkOrderStatus.CREATED);
        when(repository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkOrderView view = service.assign(tenantId, order.getId(), technicianId);

        assertEquals(WorkOrderStatus.ASSIGNED, view.status());
        assertEquals(technicianId, view.assignedToUserId());
    }

    @Test
    void startMovesToInProgress() {
        WorkOrder order = persistedOrder(WorkOrderStatus.ASSIGNED);
        when(repository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkOrderView view = service.start(tenantId, order.getId());

        assertEquals(WorkOrderStatus.IN_PROGRESS, view.status());
        assertTrue(view.startedAt() != null);
    }

    @Test
    void completeFillsCompletedAt() {
        WorkOrder order = persistedOrder(WorkOrderStatus.IN_PROGRESS);
        when(repository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkOrderView view = service.complete(tenantId, order.getId());

        assertEquals(WorkOrderStatus.COMPLETED, view.status());
        assertTrue(view.completedAt() != null);
    }

    @Test
    void cannotModifyCompletedOrder() {
        WorkOrder order = persistedOrder(WorkOrderStatus.COMPLETED);

        assertThrows(ConflictException.class, () -> service.start(tenantId, order.getId()));
        verify(repository, never()).save(any());
    }

    @Test
    void createFromAlertSkipsWhenAlreadyOpen() {
        AlertMessage alert = new AlertMessage(UUID.randomUUID(), tenantId, UUID.randomUUID(),
                "Temperature elevee", equipmentId, "temperature", 95.0, 85.0, "GT",
                "CRITICAL", "OPEN", "temperature GT 95.0 C — Temperature elevee",
                Instant.now(), Instant.now());
        when(repository.existsByTenantIdAndAlertIdAndStatusIn(eq(tenantId), eq(alert.id()), any(List.class)))
                .thenReturn(true);

        Optional<WorkOrderView> created = service.createFromAlert(tenantId, alert);

        assertTrue(created.isEmpty());
        verify(repository, never()).save(any());
    }

    @Test
    void createFromAlertCreatesSeverityOrder() {
        AlertMessage alert = new AlertMessage(UUID.randomUUID(), tenantId, UUID.randomUUID(),
                "Temperature elevee", equipmentId, "temperature", 95.0, 85.0, "GT",
                "CRITICAL", "OPEN", "temperature GT 95.0 C — Temperature elevee",
                Instant.now(), Instant.now());
        when(repository.existsByTenantIdAndAlertIdAndStatusIn(eq(tenantId), eq(alert.id()), any(List.class)))
                .thenReturn(false);
        when(repository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<WorkOrderView> created = service.createFromAlert(tenantId, alert);

        assertTrue(created.isPresent());
        assertEquals(WorkOrderSource.ALERT, created.get().source());
        assertEquals(WorkOrderPriority.CRITICAL, created.get().priority());
        assertEquals(alert.id(), created.get().alertId());
    }

    @Test
    void kpiCountsOpenAndOverdue() {
        when(repository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.CREATED)).thenReturn(2L);
        when(repository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.ASSIGNED)).thenReturn(1L);
        when(repository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.IN_PROGRESS)).thenReturn(3L);
        when(repository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.COMPLETED)).thenReturn(5L);
        when(repository.countByTenantIdAndStatusIn(eq(tenantId), any(List.class))).thenReturn(6L);
        when(repository.countByTenantIdAndStatusInAndDueAtBefore(eq(tenantId), any(List.class), any())).thenReturn(1L);
        when(repository.countByTenantIdAndCompletedAtAfter(eq(tenantId), any())).thenReturn(2L);

        var kpi = service.kpi(tenantId, 42L);

        assertEquals(2, kpi.created());
        assertEquals(6, kpi.open());
        assertEquals(1, kpi.overdue());
        assertEquals(2, kpi.completedToday());
        assertEquals(42L, kpi.downtimeTodayMinutes());
    }
}