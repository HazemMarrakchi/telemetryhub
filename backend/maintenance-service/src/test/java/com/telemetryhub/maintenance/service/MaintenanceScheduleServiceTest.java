package com.telemetryhub.maintenance.service;

import com.telemetryhub.maintenance.api.CreateScheduleRequest;
import com.telemetryhub.maintenance.api.MaintenanceScheduleView;
import com.telemetryhub.maintenance.domain.MaintenanceSchedule;
import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderType;
import com.telemetryhub.maintenance.persistence.MaintenanceScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaintenanceScheduleServiceTest {

    private final MaintenanceScheduleRepository repository = mock(MaintenanceScheduleRepository.class);
    private final MaintenanceScheduleService service = new MaintenanceScheduleService(repository);

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    void createsScheduleWithNextRunAt() {
        UUID equipmentId = UUID.randomUUID();
        CreateScheduleRequest request = new CreateScheduleRequest(
                equipmentId, "Vidange bi-mensuelle", "Remplacement du lubrifiant",
                WorkOrderType.PREVENTIVE, WorkOrderPriority.MEDIUM, 60, null);
        when(repository.save(any(MaintenanceSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceScheduleView view = service.create(tenantId, request);

        assertEquals("Vidange bi-mensuelle", view.title());
        assertEquals(equipmentId, view.equipmentId());
        assertEquals(60, view.intervalDays());
        assertEquals(WorkOrderType.PREVENTIVE, view.workType());
    }

    @Test
    void listsSchedulesForTenant() {
        MaintenanceSchedule schedule = new MaintenanceSchedule(tenantId, UUID.randomUUID(),
                "Inspection annuelle", null, WorkOrderType.INSPECTION,
                WorkOrderPriority.LOW, 365, Instant.now().plusSeconds(30L * 86400L));
        when(repository.findByTenantIdOrderByNextRunAtAsc(tenantId)).thenReturn(List.of(schedule));

        List<MaintenanceScheduleView> views = service.list(tenantId);

        assertEquals(1, views.size());
        assertEquals("Inspection annuelle", views.get(0).title());
        assertEquals(WorkOrderType.INSPECTION, views.get(0).workType());
        assertEquals(schedule.getId(), views.get(0).id());
    }

    @Test
    void deleteThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(tenantId, id));
        verify(repository, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void deleteRemovesSchedule() {
        UUID id = UUID.randomUUID();
        MaintenanceSchedule schedule = new MaintenanceSchedule(tenantId, UUID.randomUUID(),
                "Contrôle trimestriel", null, WorkOrderType.PREVENTIVE,
                WorkOrderPriority.MEDIUM, 90, Instant.now());
        when(repository.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(schedule));

        service.delete(tenantId, id);

        verify(repository).delete(schedule);
    }
}