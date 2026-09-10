package com.telemetryhub.maintenance.service;

import com.telemetryhub.maintenance.api.AvailabilityView;
import com.telemetryhub.maintenance.domain.DowntimeEvent;
import com.telemetryhub.maintenance.persistence.DowntimeEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DowntimeServiceTest {

    private final DowntimeEventRepository repository = mock(DowntimeEventRepository.class);
    private final WorkOrderService workOrderService = mock(WorkOrderService.class);
    private final DowntimeService service = new DowntimeService(repository, workOrderService);

    private UUID tenantId;
    private UUID equipmentId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();
    }

    private AlertMessage alert(String severity, String status, UUID alertId) {
        return new AlertMessage(alertId, tenantId, UUID.randomUUID(), "Temperature elevee",
                equipmentId, "temperature", 95.0, 85.0, "GT", severity, status,
                "temperature GT 95.0 C", Instant.now(), Instant.now());
    }

    @Test
    void opensDowntimeOnSevereAlert() {
        AlertMessage message = alert("CRITICAL", "OPEN", UUID.randomUUID());
        when(repository.findFirstByTenantIdAndEquipmentIdAndEndedAtIsNullOrderByStartedAtDesc(
                tenantId, equipmentId)).thenReturn(Optional.empty());
        when(workOrderService.createFromAlert(eq(tenantId), eq(message))).thenReturn(Optional.empty());
        when(repository.save(any(DowntimeEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleAlert(message);

        verify(repository).save(any(DowntimeEvent.class));
    }

    @Test
    void ignoresWarningAlerts() {
        AlertMessage message = alert("WARNING", "OPEN", UUID.randomUUID());

        service.handleAlert(message);

        verify(repository, never()).save(any());
        verify(workOrderService, never()).createFromAlert(eq(tenantId), eq(message));
    }

    @Test
    void doesNotDuplicateOpenDowntime() {
        AlertMessage message = alert("CRITICAL", "OPEN", UUID.randomUUID());
        DowntimeEvent existing = new DowntimeEvent(tenantId, equipmentId, message.id(), "temperature",
                "message", Instant.now().minusSeconds(60), null);
        when(repository.findFirstByTenantIdAndEquipmentIdAndEndedAtIsNullOrderByStartedAtDesc(
                tenantId, equipmentId)).thenReturn(Optional.of(existing));
        when(workOrderService.createFromAlert(eq(tenantId), eq(message))).thenReturn(Optional.empty());

        service.handleAlert(message);

        verify(repository, never()).save(any(DowntimeEvent.class));
    }

    @Test
    void closesDowntimeOnResolution() {
        UUID alertId = UUID.randomUUID();
        AlertMessage message = alert("CRITICAL", "RESOLVED", alertId);
        DowntimeEvent open = new DowntimeEvent(tenantId, equipmentId, alertId, "temperature",
                "message", Instant.now().minusSeconds(300), null);
        when(repository.findByTenantIdAndAlertIdAndEndedAtIsNull(tenantId, alertId))
                .thenReturn(Optional.of(open));
        when(repository.save(any(DowntimeEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleAlert(message);

        assertTrue(open.getEndedAt() != null);
        verify(repository).save(open);
    }

    @Test
    void availabilityComputesUptime() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        DowntimeEvent event = new DowntimeEvent(tenantId, equipmentId, UUID.randomUUID(), "temperature",
                "message", from.plusSeconds(600), null);
        event.setEndedAt(from.plusSeconds(1200));
        when(repository.overlapping(tenantId, equipmentId, from, to)).thenReturn(List.of(event));

        AvailabilityView view = service.availability(tenantId, equipmentId, from, to);

        assertEquals(10, view.downtimeMinutes());
        assertEquals(50, view.uptimeMinutes());
        assertEquals(60, view.totalMinutes());
        assertTrue(view.availabilityPercent() > 83.0 && view.availabilityPercent() < 84.0);
        assertEquals(1, view.downtimeCount());
    }

    @Test
    void oeeMergesOverlappingDowntime() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        DowntimeEvent e1 = new DowntimeEvent(tenantId, equipmentId, UUID.randomUUID(), "temperature",
                "message", from.plusSeconds(600), null);
        e1.setEndedAt(from.plusSeconds(1200));
        DowntimeEvent e2 = new DowntimeEvent(tenantId, UUID.randomUUID(), UUID.randomUUID(), "temperature",
                "message", from.plusSeconds(900), null);
        e2.setEndedAt(from.plusSeconds(1800));
        when(repository.overlapping(tenantId, null, from, to)).thenReturn(List.of(e1, e2));
        when(workOrderService.countCompletedBetween(tenantId, from, to)).thenReturn(2L);
        when(workOrderService.countCreatedBetween(tenantId, from, to)).thenReturn(4L);

        var view = service.oee(tenantId, from, to);

        assertEquals(20, view.downtimeMinutes());
        assertEquals(40, view.uptimeMinutes());
        assertEquals(60, view.totalMinutes());
        assertEquals(2, view.downtimeCount());
        assertEquals(50.0, view.completionRatePercent(), 0.001);
        assertTrue(view.availabilityPercent() > 66.0 && view.availabilityPercent() < 67.0);
    }
}