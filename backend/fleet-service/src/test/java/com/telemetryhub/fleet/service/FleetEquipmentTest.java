package com.telemetryhub.fleet.service;

import com.telemetryhub.fleet.api.CreateEquipmentRequest;
import com.telemetryhub.fleet.api.EquipmentView;
import com.telemetryhub.fleet.domain.Equipment;
import com.telemetryhub.fleet.domain.EquipmentModel;
import com.telemetryhub.fleet.domain.EquipmentStatus;
import com.telemetryhub.fleet.domain.Site;
import com.telemetryhub.fleet.persistence.EquipmentModelRepository;
import com.telemetryhub.fleet.persistence.EquipmentRepository;
import com.telemetryhub.fleet.persistence.SiteRepository;
import com.telemetryhub.fleet.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FleetEquipmentTest {

    private final EquipmentModelRepository modelRepo = mock(EquipmentModelRepository.class);
    private final SiteRepository siteRepo = mock(SiteRepository.class);
    private final EquipmentRepository equipmentRepo = mock(EquipmentRepository.class);
    private final FleetService service = new FleetService(modelRepo, siteRepo, equipmentRepo);

    private UUID tenantId;
    private EquipmentModel model;
    private Site site;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext(tenantId, null));
        model = makeModel("Compresseur");
        site = makeSite("Usine Sousse");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createEquipmentPersistsAndReturnsActiveStatus() {
        when(modelRepo.findByIdAndTenantId(any(), eq(tenantId))).thenReturn(Optional.of(model));
        when(siteRepo.findByIdAndTenantId(any(), eq(tenantId))).thenReturn(Optional.of(site));
        when(equipmentRepo.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateEquipmentRequest request = new CreateEquipmentRequest(
                "SAS-01", "SN-001", model.getId(), site.getId(), null, null);

        EquipmentView view = service.createEquipment(request);

        assertEquals("SAS-01", view.name());
        assertEquals(EquipmentStatus.ACTIVE, view.status());
    }

    @Test
    void heartbeatUpdatesLastSeenAt() {
        Equipment eq = makeEquipment("SAS-01", EquipmentStatus.ACTIVE);
        when(equipmentRepo.findByIdAndTenantId(eq.getId(), tenantId)).thenReturn(Optional.of(eq));
        when(equipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.heartbeat(eq.getId());

        assertNotNull(eq.getLastSeenAt());
        verify(equipmentRepo).save(eq);
    }

    @Test
    void changeEquipmentStatusTransitionsCorrectly() {
        Equipment eq = makeEquipment("SAS-01", EquipmentStatus.ACTIVE);
        when(equipmentRepo.findByIdAndTenantId(eq.getId(), tenantId)).thenReturn(Optional.of(eq));
        when(equipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EquipmentView view = service.changeEquipmentStatus(eq.getId(), EquipmentStatus.MAINTENANCE);

        assertEquals(EquipmentStatus.MAINTENANCE, view.status());
    }

    @Test
    void maintenanceDueReturnsEquipmentWithUpcomingMaintenance() {
        Equipment eq = makeEquipment("SAS-01", EquipmentStatus.ACTIVE);
        eq.setNextMaintenanceAt(Instant.now().plusSeconds(3600));
        when(equipmentRepo.findByNextMaintenanceAtBeforeAndTenantId(any(), eq(tenantId)))
                .thenReturn(java.util.List.of(eq));

        var result = service.maintenanceDue(tenantId);

        assertEquals(1, result.size());
    }

    @Test
    void roadmapMaintenanceSetsNextMaintenanceDate() {
        Equipment eq = makeEquipment("SAS-01", EquipmentStatus.ACTIVE);
        when(equipmentRepo.findByIdAndTenantId(eq.getId(), tenantId)).thenReturn(Optional.of(eq));
        when(equipmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Instant next = Instant.parse("2026-12-01T08:00:00Z");

        service.roadmapMaintenance(eq.getId(), next);

        assertEquals(next, eq.getNextMaintenanceAt());
        verify(equipmentRepo).save(eq);
    }

    private EquipmentModel makeModel(String name) {
        EquipmentModel m = new EquipmentModel(tenantId, name, "Mfr", "Desc", "ROTATING");
        m.setId(UUID.randomUUID());
        m.setActive(true);
        return m;
    }

    private Site makeSite(String name) {
        Site s = new Site(tenantId, name, "Addr", "City", "Country", 35.0, 10.0);
        s.setId(UUID.randomUUID());
        return s;
    }

    private Equipment makeEquipment(String name, EquipmentStatus status) {
        Equipment e = new Equipment(tenantId, name, "SN-" + name, null, null);
        e.setId(UUID.randomUUID());
        e.setStatus(status);
        return e;
    }
}