package com.telemetryhub.maintenance.api;

import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderStatus;
import com.telemetryhub.maintenance.security.TenantContext;
import com.telemetryhub.maintenance.service.DowntimeService;
import com.telemetryhub.maintenance.service.MaintenanceScheduleService;
import com.telemetryhub.maintenance.service.WorkOrderService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("isAuthenticated()")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final DowntimeService downtimeService;
    private final MaintenanceScheduleService maintenanceScheduleService;

    public WorkOrderController(WorkOrderService workOrderService, DowntimeService downtimeService,
                               MaintenanceScheduleService maintenanceScheduleService) {
        this.workOrderService = workOrderService;
        this.downtimeService = downtimeService;
        this.maintenanceScheduleService = maintenanceScheduleService;
    }

    @GetMapping("/work-orders")
    public ResponseEntity<Page<WorkOrderView>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) WorkOrderPriority priority,
            @RequestParam(required = false) UUID equipmentId) {
        return ResponseEntity.ok(workOrderService.list(
                TenantContext.tenantId(), page, size, sort, status, priority, equipmentId));
    }

    @GetMapping("/work-orders/kpi")
    public ResponseEntity<KpiView> kpi() {
        UUID tenantId = TenantContext.tenantId();
        return ResponseEntity.ok(workOrderService.kpi(
                tenantId, downtimeService.downtimeTodayMinutes(tenantId)));
    }

    @GetMapping("/work-orders/history")
    public ResponseEntity<Page<WorkOrderView>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(workOrderService.history(TenantContext.tenantId(), page, size));
    }

    @GetMapping("/work-orders/costs")
    public ResponseEntity<CostTrendView> costs() {
        return ResponseEntity.ok(workOrderService.costTrends(TenantContext.tenantId()));
    }

    @GetMapping("/work-orders/schedules")
    public ResponseEntity<List<MaintenanceScheduleView>> schedules() {
        return ResponseEntity.ok(maintenanceScheduleService.list(TenantContext.tenantId()));
    }

    @PostMapping("/work-orders/schedules")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<MaintenanceScheduleView> createSchedule(
            @Valid @RequestBody CreateScheduleRequest request) {
        MaintenanceScheduleView view = maintenanceScheduleService.create(TenantContext.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @DeleteMapping("/work-orders/schedules/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        maintenanceScheduleService.delete(TenantContext.tenantId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/work-orders/{id}")
    public ResponseEntity<WorkOrderView> get(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.get(TenantContext.tenantId(), id));
    }

    @PostMapping("/work-orders")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<WorkOrderView> create(@Valid @RequestBody CreateWorkOrderRequest request) {
        WorkOrderView view = workOrderService.create(TenantContext.tenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PatchMapping("/work-orders/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<WorkOrderView> update(@PathVariable UUID id,
                                                @RequestBody UpdateWorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.update(TenantContext.tenantId(), id, request));
    }

    @PostMapping("/work-orders/{id}/assign")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<WorkOrderView> assign(@PathVariable UUID id,
                                                @Valid @RequestBody AssignWorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.assign(
                TenantContext.tenantId(), id, request.assignedToUserId()));
    }

    @PostMapping("/work-orders/{id}/start")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<WorkOrderView> start(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.start(TenantContext.tenantId(), id));
    }

    @PostMapping("/work-orders/{id}/complete")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<WorkOrderView> complete(@PathVariable UUID id,
                                                  @RequestBody(required = false)
                                                  CompleteWorkOrderRequest request) {
        String notes = request != null ? request.completionNotes() : null;
        return ResponseEntity.ok(workOrderService.complete(TenantContext.tenantId(), id, notes));
    }

    @PostMapping("/work-orders/{id}/cancel")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','OPERATOR')")
    public ResponseEntity<WorkOrderView> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(workOrderService.cancel(TenantContext.tenantId(), id));
    }

    @GetMapping("/downtime/oee")
    public ResponseEntity<GlobalOeeView> oee(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(downtimeService.oee(TenantContext.tenantId(), from, to));
    }

    @GetMapping("/downtime/equipments/{equipmentId}/availability")
    public ResponseEntity<AvailabilityView> availability(
            @PathVariable UUID equipmentId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(downtimeService.availability(
                TenantContext.tenantId(), equipmentId, from, to));
    }
}