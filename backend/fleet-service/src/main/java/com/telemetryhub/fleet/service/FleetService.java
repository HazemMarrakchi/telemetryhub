package com.telemetryhub.fleet.service;

import com.telemetryhub.fleet.api.CreateEquipmentRequest;
import com.telemetryhub.fleet.api.CreateSiteRequest;
import com.telemetryhub.fleet.api.EquipmentModelRequest;
import com.telemetryhub.fleet.api.EquipmentView;
import com.telemetryhub.fleet.api.SiteView;
import com.telemetryhub.fleet.api.ModelView;
import com.telemetryhub.fleet.domain.Equipment;
import com.telemetryhub.fleet.domain.EquipmentModel;
import com.telemetryhub.fleet.domain.EquipmentStatus;
import com.telemetryhub.fleet.domain.Site;
import com.telemetryhub.fleet.persistence.EquipmentModelRepository;
import com.telemetryhub.fleet.persistence.EquipmentRepository;
import com.telemetryhub.fleet.persistence.SiteRepository;
import com.telemetryhub.fleet.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FleetService {

    private final EquipmentModelRepository modelRepository;
    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;

    public FleetService(EquipmentModelRepository modelRepository,
                        SiteRepository siteRepository,
                        EquipmentRepository equipmentRepository) {
        this.modelRepository = modelRepository;
        this.siteRepository = siteRepository;
        this.equipmentRepository = equipmentRepository;
    }

    private UUID tenantId() {
        return TenantContext.get().getTenantId();
    }

    // ===================== Models =====================

    @Transactional
    public ModelView createModel(EquipmentModelRequest request) {
        EquipmentModel model = new EquipmentModel(
                tenantId(), request.name(), request.manufacturer(),
                request.description(), request.category());
        return ModelView.from(modelRepository.save(model));
    }

    @Transactional(readOnly = true)
    public Page<ModelView> listModels(int page, int size, String sort) {
        Sort sortSpec = Sort.by(sort == null ? "name" : sort).ascending();
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return modelRepository.findByTenantId(tenantId(), pageable).map(ModelView::from);
    }

    @Transactional(readOnly = true)
    public List<ModelView> listActiveModels() {
        return modelRepository.findByTenantIdAndActiveTrue(tenantId())
                .stream().map(ModelView::from).toList();
    }

    @Transactional(readOnly = true)
    public ModelView getModel(UUID modelId) {
        return ModelView.from(findModel(modelId));
    }

    @Transactional
    public ModelView updateModel(UUID modelId, EquipmentModelRequest request) {
        EquipmentModel model = findModel(modelId);
        model.setName(request.name());
        model.setManufacturer(request.manufacturer());
        model.setDescription(request.description());
        model.setCategory(request.category());
        return ModelView.from(modelRepository.save(model));
    }

    @Transactional
    public void deleteModel(UUID modelId) {
        EquipmentModel model = findModel(modelId);
        model.setActive(false);
        modelRepository.save(model);
    }

    // ===================== Sites =====================

    @Transactional
    public SiteView createSite(CreateSiteRequest request) {
        Site site = new Site(tenantId(), request.name(), request.address(),
                request.city(), request.country(),
                request.latitude(), request.longitude());
        return SiteView.from(siteRepository.save(site));
    }

    @Transactional(readOnly = true)
    public Page<SiteView> listSites(int page, int size, String sort) {
        Sort sortSpec = Sort.by(sort == null ? "name" : sort).ascending();
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return siteRepository.findByTenantId(tenantId(), pageable).map(SiteView::from);
    }

    @Transactional(readOnly = true)
    public List<SiteView> listActiveSites() {
        return siteRepository.findByTenantIdAndActiveTrue(tenantId())
                .stream().map(SiteView::from).toList();
    }

    @Transactional(readOnly = true)
    public SiteView getSite(UUID siteId) {
        return SiteView.from(findSite(siteId));
    }

    @Transactional
    public SiteView updateSite(UUID siteId, CreateSiteRequest request) {
        Site site = findSite(siteId);
        site.setName(request.name());
        site.setAddress(request.address());
        site.setCity(request.city());
        site.setCountry(request.country());
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        return SiteView.from(siteRepository.save(site));
    }

    @Transactional
    public void deleteSite(UUID siteId) {
        Site site = findSite(siteId);
        site.setActive(false);
        siteRepository.save(site);
    }

    // ===================== Equipments =====================

    @Transactional
    public EquipmentView createEquipment(CreateEquipmentRequest request) {
        if (equipmentRepository.existsBySerialNumber(request.serialNumber())) {
            throw new ConflictException("Le numéro de série existe déjà: " + request.serialNumber());
        }
        EquipmentModel model = findModel(request.modelId());
        Site site = findSite(request.siteId());
        Equipment equipment = new Equipment(
                tenantId(), request.name(), request.serialNumber(), model, site);
        equipment.setNotes(request.notes());
        if (request.nextMaintenanceAt() != null) {
            equipment.setNextMaintenanceAt(request.nextMaintenanceAt());
        }
        return EquipmentView.from(equipmentRepository.save(equipment));
    }

    @Transactional(readOnly = true)
    public Page<EquipmentView> listEquipments(int page, int size, EquipmentStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Equipment> result = status == null
                ? equipmentRepository.findByTenantId(tenantId(), pageable)
                : equipmentRepository.findByTenantIdAndStatus(tenantId(), status, pageable);
        return result.map(EquipmentView::from);
    }

    @Transactional(readOnly = true)
    public List<EquipmentView> listEquipmentsBySite(UUID siteId) {
        Site site = findSite(siteId);
        return equipmentRepository.findByTenantIdAndSiteId(tenantId(), site.getId())
                .stream().map(EquipmentView::from).toList();
    }

    @Transactional(readOnly = true)
    public EquipmentView getEquipment(UUID equipmentId) {
        return EquipmentView.from(findEquipment(equipmentId));
    }

    @Transactional
    public EquipmentView updateEquipment(UUID equipmentId, CreateEquipmentRequest request) {
        Equipment equipment = findEquipment(equipmentId);
        equipment.setName(request.name());
        equipment.setModel(findModel(request.modelId()));
        equipment.setSite(findSite(request.siteId()));
        equipment.setNotes(request.notes());
        if (request.nextMaintenanceAt() != null) {
            equipment.setNextMaintenanceAt(request.nextMaintenanceAt());
        }
        return EquipmentView.from(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentView changeEquipmentStatus(UUID equipmentId, EquipmentStatus status) {
        Equipment equipment = findEquipment(equipmentId);
        equipment.changeStatus(status);
        return EquipmentView.from(equipmentRepository.save(equipment));
    }

    @Transactional
    public void heartbeat(UUID equipmentId) {
        Equipment equipment = findEquipment(equipmentId);
        equipment.markSeen();
        equipmentRepository.save(equipment);
    }

    @Transactional(readOnly = true)
    public List<EquipmentView> maintenanceDue(UUID tenantId) {
        Instant threshold = Instant.now().plusSeconds(7L * 24 * 3600);
        return equipmentRepository.findByNextMaintenanceAtBeforeAndTenantId(threshold, tenantId)
                .stream().map(EquipmentView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EquipmentView> listByStatusIn(List<EquipmentStatus> statuses) {
        return equipmentRepository.findByTenantIdAndStatusIn(tenantId(), statuses)
                .stream().map(EquipmentView::from).toList();
    }

    @Transactional
    public void roadmapMaintenance(UUID equipmentId, Instant nextMaintenanceAt) {
        Equipment equipment = findEquipment(equipmentId);
        equipment.setNextMaintenanceAt(nextMaintenanceAt);
        equipmentRepository.save(equipment);
    }

    private EquipmentModel findModel(UUID modelId) {
        return modelRepository.findByIdAndTenantId(modelId, tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Modèle d'équipement introuvable"));
    }

    private Site findSite(UUID siteId) {
        return siteRepository.findByIdAndTenantId(siteId, tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Site introuvable"));
    }

    private Equipment findEquipment(UUID equipmentId) {
        return equipmentRepository.findByIdAndTenantId(equipmentId, tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Équipement introuvable"));
    }
}