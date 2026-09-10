package com.telemetryhub.maintenance.service;

import com.telemetryhub.maintenance.api.CreateWorkOrderRequest;
import com.telemetryhub.maintenance.api.KpiView;
import com.telemetryhub.maintenance.api.UpdateWorkOrderRequest;
import com.telemetryhub.maintenance.api.WorkOrderView;
import com.telemetryhub.maintenance.domain.WorkOrder;
import com.telemetryhub.maintenance.domain.WorkOrderPriority;
import com.telemetryhub.maintenance.domain.WorkOrderSource;
import com.telemetryhub.maintenance.domain.WorkOrderStatus;
import com.telemetryhub.maintenance.domain.WorkOrderType;
import com.telemetryhub.maintenance.persistence.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkOrderService {

    private static final List<WorkOrderStatus> OPEN_STATUSES =
            List.of(WorkOrderStatus.CREATED, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS);

    private final WorkOrderRepository repository;

    public WorkOrderService(WorkOrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderView> list(UUID tenantId, int page, int size, String sort,
                                    WorkOrderStatus status, WorkOrderPriority priority,
                                    UUID equipmentId) {
        Sort sortSpec = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            if (List.of("createdAt", "dueAt", "priority", "status", "updatedAt").contains(field)) {
                Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                        ? Sort.Direction.ASC : Sort.Direction.DESC;
                sortSpec = Sort.by(direction, field);
            }
        }
        PageRequest pageable = PageRequest.of(page, Math.min(size, 200), sortSpec);
        return repository.search(tenantId, status, priority, equipmentId, pageable)
                .map(WorkOrderView::from);
    }

    @Transactional(readOnly = true)
    public WorkOrderView get(UUID tenantId, UUID id) {
        return WorkOrderView.from(find(tenantId, id));
    }

    @Transactional
    public WorkOrderView create(UUID tenantId, CreateWorkOrderRequest request) {
        WorkOrder order = new WorkOrder(
                tenantId, request.equipmentId(), request.title().trim(), request.description(),
                request.priority(), WorkOrderSource.MANUAL, request.workType(), null, request.dueAt());
        return WorkOrderView.from(repository.save(order));
    }

    @Transactional
    public Optional<WorkOrderView> createFromAlert(UUID tenantId, AlertMessage alert) {
        boolean existing = repository.existsByTenantIdAndAlertIdAndStatusIn(tenantId, alert.id(), OPEN_STATUSES);
        if (existing) {
            return Optional.empty();
        }
        WorkOrderPriority priority = alert.isSevere()
                ? WorkOrderPriority.CRITICAL : WorkOrderPriority.HIGH;
        WorkOrder order = new WorkOrder(
                tenantId, alert.equipmentId(),
                "Ordre automatique: " + alert.ruleName(),
                alert.message() != null ? alert.message() : alert.ruleName(),
                priority, WorkOrderSource.ALERT, WorkOrderType.CORRECTIVE, alert.id(), null);
        return Optional.of(WorkOrderView.from(repository.save(order)));
    }

    @Transactional
    public WorkOrderView update(UUID tenantId, UUID id, UpdateWorkOrderRequest request) {
        WorkOrder order = find(tenantId, id);
        ensureMutable(order);
        if (request.title() != null && !request.title().isBlank()) {
            order.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            order.setDescription(request.description());
        }
        if (request.priority() != null) {
            order.setPriority(request.priority());
        }
        if (request.workType() != null) {
            order.setWorkType(request.workType());
        }
        if (request.equipmentId() != null) {
            order.setEquipmentId(request.equipmentId());
        }
        if (request.dueAt() != null) {
            order.setDueAt(request.dueAt());
        }
        if (request.spareParts() != null) {
            order.setSpareParts(request.spareParts());
        }
        if (request.costEstimate() != null) {
            order.setCostEstimate(request.costEstimate());
        }
        return WorkOrderView.from(repository.save(order));
    }

    @Transactional
    public WorkOrderView assign(UUID tenantId, UUID id, UUID technicianId) {
        WorkOrder order = find(tenantId, id);
        ensureMutable(order);
        if (order.getStatus() == WorkOrderStatus.CREATED) {
            order.setStatus(WorkOrderStatus.ASSIGNED);
        }
        order.setAssignedToUserId(technicianId);
        return WorkOrderView.from(repository.save(order));
    }

    @Transactional
    public WorkOrderView start(UUID tenantId, UUID id) {
        WorkOrder order = find(tenantId, id);
        ensureMutable(order);
        if (order.getStatus() == WorkOrderStatus.COMPLETED || order.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new ConflictException("Un ordre de travail terminé ne peut pas être démarré");
        }
        if (order.getStartedAt() == null) {
            order.setStartedAt(Instant.now());
        }
        order.setStatus(WorkOrderStatus.IN_PROGRESS);
        return WorkOrderView.from(repository.save(order));
    }

    @Transactional
    public WorkOrderView complete(UUID tenantId, UUID id, String completionNotes) {
        WorkOrder order = find(tenantId, id);
        ensureMutable(order);
        if (order.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new ConflictException("Un ordre de travail annulé ne peut pas être complété");
        }
        if (order.getStartedAt() == null) {
            order.setStartedAt(Instant.now());
        }
        order.setStatus(WorkOrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        if (completionNotes != null && !completionNotes.isBlank()) {
            order.setCompletionNotes(completionNotes.trim());
        }
        return WorkOrderView.from(repository.save(order));
    }

    @Transactional
    public WorkOrderView cancel(UUID tenantId, UUID id) {
        WorkOrder order = find(tenantId, id);
        ensureMutable(order);
        order.setStatus(WorkOrderStatus.CANCELLED);
        return WorkOrderView.from(repository.save(order));
    }

    @Transactional(readOnly = true)
    public KpiView kpi(UUID tenantId, long downtimeTodayMinutes) {
        Instant now = Instant.now();
        Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        return new KpiView(
                repository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.CREATED),
                repository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.ASSIGNED),
                repository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.IN_PROGRESS),
                repository.countByTenantIdAndStatusIn(tenantId, OPEN_STATUSES),
                repository.countByTenantIdAndStatusInAndDueAtBefore(tenantId, OPEN_STATUSES, now),
                repository.countByTenantIdAndCompletedAtAfter(tenantId, todayStart),
                repository.countByTenantIdAndStatus(tenantId, WorkOrderStatus.COMPLETED),
                downtimeTodayMinutes);
    }

    @Transactional(readOnly = true)
    public long countCompletedBetween(UUID tenantId, Instant from, Instant to) {
        return repository.countByTenantIdAndCompletedAtBetween(tenantId, from, to);
    }

    @Transactional(readOnly = true)
    public long countCreatedBetween(UUID tenantId, Instant from, Instant to) {
        return repository.countByTenantIdAndCreatedAtBetween(tenantId, from, to);
    }

    private WorkOrder find(UUID tenantId, UUID id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordre de travail introuvable: " + id));
    }

    private void ensureMutable(WorkOrder order) {
        if (order.getStatus() == WorkOrderStatus.COMPLETED || order.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new ConflictException("Un ordre de travail " + order.getStatus().name().toLowerCase()
                    + " ne peut plus être modifié");
        }
    }
}