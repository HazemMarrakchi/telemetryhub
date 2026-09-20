package com.telemetryhub.fleet.service;

import com.telemetryhub.fleet.api.CreateSiteRequest;
import com.telemetryhub.fleet.api.SiteView;
import com.telemetryhub.fleet.api.ModelView;
import com.telemetryhub.fleet.domain.EquipmentModel;
import com.telemetryhub.fleet.domain.Site;
import com.telemetryhub.fleet.persistence.EquipmentModelRepository;
import com.telemetryhub.fleet.persistence.EquipmentRepository;
import com.telemetryhub.fleet.persistence.SiteRepository;
import com.telemetryhub.fleet.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FleetServiceTest {

    private final EquipmentModelRepository modelRepo = mock(EquipmentModelRepository.class);
    private final SiteRepository siteRepo = mock(SiteRepository.class);
    private final EquipmentRepository equipmentRepo = mock(EquipmentRepository.class);
    private final FleetService service = new FleetService(modelRepo, siteRepo, equipmentRepo);

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.set(new TenantContext(tenantId, null));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ===================== Models =====================

    @Test
    void createModelPersistsAndReturnsView() {
        when(modelRepo.save(any(EquipmentModel.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelView view = service.createModel(
                new com.telemetryhub.fleet.api.EquipmentModelRequest(
                        "Compresseur", "Atlas", "desc", "ROTATING"));

        assertEquals("Compresseur", view.name());
        verify(modelRepo).save(any(EquipmentModel.class));
    }

    @Test
    void listModelsReturnsPagedResults() {
        EquipmentModel model = makeModel("Compresseur");
        when(modelRepo.findByTenantId(eq(tenantId), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(model)));

        var result = service.listModels(0, 20, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("Compresseur", result.getContent().get(0).name());
    }

    @Test
    void listActiveModelsReturnsOnlyActive() {
        EquipmentModel model = makeModel("Pompe");
        when(modelRepo.findByTenantIdAndActiveTrue(tenantId)).thenReturn(java.util.List.of(model));

        var result = service.listActiveModels();

        assertEquals(1, result.size());
        assertEquals("Pompe", result.get(0).name());
    }

    @Test
    void deleteModelSoftDeletes() {
        EquipmentModel mdl = makeModel("Compresseur");
        when(modelRepo.findByIdAndTenantId(any(), eq(tenantId))).thenReturn(java.util.Optional.of(mdl));
        when(modelRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteModel(mdl.getId());

        assertFalse(mdl.isActive());
        verify(modelRepo).save(mdl);
    }

    // ===================== Sites =====================

    @Test
    void createSitePersistsAndReturnsView() {
        when(siteRepo.save(any(Site.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteView view = service.createSite(new CreateSiteRequest(
                "Usine Sousse", "Rue de la ceinture", "Sousse", "Tunisie", 35.8, 10.6));

        assertEquals("Usine Sousse", view.name());
        assertEquals("Sousse", view.city());
        verify(siteRepo).save(any(Site.class));
    }

    // ===================== Helpers =====================

    private EquipmentModel makeModel(String name) {
        EquipmentModel m = new EquipmentModel(tenantId, name, "Mfr", "Desc", "ROTATING");
        m.setId(UUID.randomUUID());
        m.setActive(true);
        return m;
    }
}