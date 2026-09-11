package com.telemetryhub.maintenance.service;

import com.telemetryhub.maintenance.api.CreateScheduleRequest;
import com.telemetryhub.maintenance.api.MaintenanceScheduleView;
import com.telemetryhub.maintenance.domain.MaintenanceSchedule;
import com.telemetryhub.maintenance.persistence.MaintenanceScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MaintenanceScheduleService {

    private final MaintenanceScheduleRepository repository;

    public MaintenanceScheduleService(MaintenanceScheduleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MaintenanceScheduleView> list(UUID tenantId) {
        return repository.findByTenantIdOrderByNextRunAtAsc(tenantId)
                .stream()
                .map(MaintenanceScheduleView::from)
                .toList();
    }

    @Transactional
    public MaintenanceScheduleView create(UUID tenantId, CreateScheduleRequest request) {
        Instant nextRunAt = request.startsAt() != null ? request.startsAt() : Instant.now();
        MaintenanceSchedule schedule = new MaintenanceSchedule(
                tenantId, request.equipmentId(), request.title().trim(), request.description(),
                request.workType(), request.priority(), request.intervalDays(), nextRunAt);
        return MaintenanceScheduleView.from(repository.save(schedule));
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        MaintenanceSchedule schedule = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Planning de maintenance introuvable: " + id));
        repository.delete(schedule);
    }
}