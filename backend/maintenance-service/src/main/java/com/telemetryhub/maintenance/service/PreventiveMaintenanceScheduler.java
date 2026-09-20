package com.telemetryhub.maintenance.service;

import com.telemetryhub.maintenance.api.WorkOrderView;
import com.telemetryhub.maintenance.domain.MaintenanceSchedule;
import com.telemetryhub.maintenance.persistence.MaintenanceScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PreventiveMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(PreventiveMaintenanceScheduler.class);

    private final MaintenanceScheduleRepository scheduleRepository;
    private final WorkOrderService workOrderService;
    private final Clock clock;

    public PreventiveMaintenanceScheduler(MaintenanceScheduleRepository scheduleRepository,
                                          WorkOrderService workOrderService,
                                          Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.workOrderService = workOrderService;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void generateDueWorkOrders() {
        Instant now = clock.instant();
        List<MaintenanceSchedule> due = scheduleRepository.findByActiveTrueAndNextRunAtBefore(now);
        for (MaintenanceSchedule schedule : due) {
            try {
                WorkOrderView created = workOrderService.createFromSchedule(schedule.getTenantId(), schedule);
                if (created == null) {
                    continue;
                }
                schedule.setLastRunAt(now);
                schedule.setNextRunAt(now.plus(schedule.getIntervalDays(), ChronoUnit.DAYS));
                scheduleRepository.save(schedule);
            } catch (Exception ex) {
                log.error("Erreur lors de la génération préventive du planning {}: {}",
                        schedule.getId(), ex.getMessage(), ex);
            }
        }
    }
}