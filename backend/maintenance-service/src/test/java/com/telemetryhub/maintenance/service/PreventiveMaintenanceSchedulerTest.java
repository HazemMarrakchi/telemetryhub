package com.telemetryhub.maintenance.service;

import com.telemetryhub.maintenance.api.WorkOrderView;
import com.telemetryhub.maintenance.domain.MaintenanceSchedule;
import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderType;
import com.telemetryhub.maintenance.persistence.MaintenanceScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreventiveMaintenanceSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-09-10T10:00:00Z");

    private final MaintenanceScheduleRepository repository = mock(MaintenanceScheduleRepository.class);
    private final WorkOrderService workOrderService = mock(WorkOrderService.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final PreventiveMaintenanceScheduler scheduler =
            new PreventiveMaintenanceScheduler(repository, workOrderService, clock);

    private UUID tenantId;
    private MaintenanceSchedule schedule;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        schedule = new MaintenanceSchedule(tenantId, UUID.randomUUID(),
                "Vidange bi-mensuelle", null, WorkOrderType.PREVENTIVE,
                WorkOrderPriority.MEDIUM, 30, NOW.minusSeconds(86400));
        schedule.setActive(true);
        when(repository.findByActiveTrueAndNextRunAtBefore(any(Instant.class))).thenReturn(List.of(schedule));
    }

    @Test
    void generatesOrderAndAdvancesSchedule() {
        when(workOrderService.createFromSchedule(eq(tenantId), eq(schedule))).thenReturn(mock(WorkOrderView.class));
        when(repository.save(any(MaintenanceSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduler.generateDueWorkOrders();

        assertEquals(NOW, schedule.getLastRunAt());
        assertEquals(NOW.plusSeconds(30L * 86400L), schedule.getNextRunAt());
        verify(repository).save(schedule);
    }

    @Test
    void skipsAdvanceWhenOrderAlreadyOpen() {
        when(workOrderService.createFromSchedule(eq(tenantId), eq(schedule))).thenReturn(null);

        scheduler.generateDueWorkOrders();

        assertEquals(NOW.minusSeconds(86400), schedule.getNextRunAt());
        verify(repository, never()).save(any(MaintenanceSchedule.class));
    }

    @Test
    void toleratesPerScheduleFailure() {
        MaintenanceSchedule other = new MaintenanceSchedule(tenantId, UUID.randomUUID(),
                "Inspection annuelle", null, WorkOrderType.INSPECTION,
                WorkOrderPriority.LOW, 365, NOW.minusSeconds(3600));
        when(repository.findByActiveTrueAndNextRunAtBefore(any(Instant.class))).thenReturn(List.of(schedule, other));
        when(workOrderService.createFromSchedule(eq(tenantId), eq(schedule)))
                .thenThrow(new RuntimeException("boom"));
        when(workOrderService.createFromSchedule(eq(tenantId), eq(other))).thenReturn(mock(WorkOrderView.class));
        when(repository.save(any(MaintenanceSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduler.generateDueWorkOrders();

        verify(repository).save(other);
        verify(repository, never()).save(schedule);
    }
}