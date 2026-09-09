package com.telemetryhub.fleet.api;

import com.telemetryhub.fleet.domain.EquipmentStatus;
import com.telemetryhub.fleet.service.FleetService;
import com.telemetryhub.fleet.service.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FleetController {

    private final FleetService fleetService;

    public FleetController(FleetService fleetService) {
        this.fleetService = fleetService;
    }

    // ===================== Models =====================

    @PostMapping("/models")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<ModelView> createModel(@Valid @RequestBody EquipmentModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fleetService.createModel(request));
    }

    @GetMapping("/models")
    public ResponseEntity<Page<ModelView>> listModels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(fleetService.listModels(page, Math.min(size, 100), sort));
    }

    @GetMapping("/models/active")
    public ResponseEntity<List<ModelView>> listActiveModels() {
        return ResponseEntity.ok(fleetService.listActiveModels());
    }

    @GetMapping("/models/{modelId}")
    public ResponseEntity<ModelView> getModel(@PathVariable UUID modelId) {
        return ResponseEntity.ok(fleetService.getModel(modelId));
    }

    @PutMapping("/models/{modelId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<ModelView> updateModel(@PathVariable UUID modelId,
                                                 @Valid @RequestBody EquipmentModelRequest request) {
        return ResponseEntity.ok(fleetService.updateModel(modelId, request));
    }

    @DeleteMapping("/models/{modelId}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Void> deleteModel(@PathVariable UUID modelId) {
        fleetService.deleteModel(modelId);
        return ResponseEntity.noContent().build();
    }

    // ===================== Sites =====================

    @PostMapping("/sites")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<SiteView> createSite(@Valid @RequestBody CreateSiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fleetService.createSite(request));
    }

    @GetMapping("/sites")
    public ResponseEntity<Page<SiteView>> listSites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(fleetService.listSites(page, Math.min(size, 100), sort));
    }

    @GetMapping("/sites/active")
    public ResponseEntity<List<SiteView>> listActiveSites() {
        return ResponseEntity.ok(fleetService.listActiveSites());
    }

    @GetMapping("/sites/{siteId}")
    public ResponseEntity<SiteView> getSite(@PathVariable UUID siteId) {
        return ResponseEntity.ok(fleetService.getSite(siteId));
    }

    @PutMapping("/sites/{siteId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<SiteView> updateSite(@PathVariable UUID siteId,
                                               @Valid @RequestBody CreateSiteRequest request) {
        return ResponseEntity.ok(fleetService.updateSite(siteId, request));
    }

    @DeleteMapping("/sites/{siteId}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Void> deleteSite(@PathVariable UUID siteId) {
        fleetService.deleteSite(siteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sites/{siteId}/equipments")
    public ResponseEntity<List<EquipmentView>> listEquipmentsBySite(@PathVariable UUID siteId) {
        return ResponseEntity.ok(fleetService.listEquipmentsBySite(siteId));
    }

    // ===================== Equipments =====================

    @PostMapping("/equipments")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<EquipmentView> createEquipment(@Valid @RequestBody CreateEquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fleetService.createEquipment(request));
    }

    @GetMapping("/equipments")
    public ResponseEntity<Page<EquipmentView>> listEquipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) EquipmentStatus status) {
        return ResponseEntity.ok(fleetService.listEquipments(page, Math.min(size, 100), status));
    }

    @GetMapping("/equipments/{equipmentId}")
    public ResponseEntity<EquipmentView> getEquipment(@PathVariable UUID equipmentId) {
        return ResponseEntity.ok(fleetService.getEquipment(equipmentId));
    }

    @PutMapping("/equipments/{equipmentId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<EquipmentView> updateEquipment(@PathVariable UUID equipmentId,
                                                         @Valid @RequestBody CreateEquipmentRequest request) {
        return ResponseEntity.ok(fleetService.updateEquipment(equipmentId, request));
    }

    @PatchMapping("/equipments/{equipmentId}/status")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<EquipmentView> changeStatus(@PathVariable UUID equipmentId,
                                                      @RequestParam EquipmentStatus status) {
        return ResponseEntity.ok(fleetService.changeEquipmentStatus(equipmentId, status));
    }

    @PostMapping("/equipments/{equipmentId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable UUID equipmentId) {
        fleetService.heartbeat(equipmentId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/equipments/{equipmentId}/maintenance")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<EquipmentView> scheduleMaintenance(
            @PathVariable UUID equipmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant nextMaintenanceAt) {
        fleetService.roadmapMaintenance(equipmentId, nextMaintenanceAt);
        return ResponseEntity.ok(fleetService.getEquipment(equipmentId));
    }

    @GetMapping("/maintenance/due")
    public ResponseEntity<List<EquipmentView>> maintenanceDue() {
        return ResponseEntity.ok(fleetService.listByStatusIn(
                List.of(EquipmentStatus.ACTIVE, EquipmentStatus.MAINTENANCE)));
    }
}