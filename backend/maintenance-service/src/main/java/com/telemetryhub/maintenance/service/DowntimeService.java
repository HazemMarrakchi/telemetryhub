package com.telemetryhub.maintenance.service;

import com.telemetryhub.maintenance.api.AvailabilityView;
import com.telemetryhub.maintenance.api.GlobalOeeView;
import com.telemetryhub.maintenance.domain.DowntimeEvent;
import com.telemetryhub.maintenance.persistence.DowntimeEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DowntimeService {

    private static final Logger log = LoggerFactory.getLogger(DowntimeService.class);

    private final DowntimeEventRepository repository;
    private final WorkOrderService workOrderService;

    public DowntimeService(DowntimeEventRepository repository, WorkOrderService workOrderService) {
        this.repository = repository;
        this.workOrderService = workOrderService;
    }

    @Transactional
    public void handleAlert(AlertMessage alert) {
        if (alert.tenantId() == null || alert.equipmentId() == null) {
            return;
        }
        if (alert.isOpen() && alert.isSevere()) {
            openDowntime(alert);
        } else if (alert.isResolved()) {
            closeDowntime(alert);
        }
    }

    private void openDowntime(AlertMessage alert) {
        DowntimeEvent existing = repository
                .findFirstByTenantIdAndEquipmentIdAndEndedAtIsNullOrderByStartedAtDesc(
                        alert.tenantId(), alert.equipmentId())
                .orElse(null);
        UUID workOrderId = null;
        var created = workOrderService.createFromAlert(alert.tenantId(), alert);
        if (created.isPresent()) {
            workOrderId = created.get().id();
        }
        if (existing == null) {
            Instant started = alert.triggeredAt() != null ? alert.triggeredAt() : Instant.now();
            DowntimeEvent event = new DowntimeEvent(
                    alert.tenantId(), alert.equipmentId(), alert.id(), alert.metric(),
                    alert.message(), started, workOrderId);
            repository.save(event);
            log.warn("Temps d'arret ouvert (equipement {}, alerte {})",
                    alert.equipmentId(), alert.id());
        } else if (workOrderId != null) {
            existing.setWorkOrderId(workOrderId);
            repository.save(existing);
        }
    }

    private void closeDowntime(AlertMessage alert) {
        repository.findByTenantIdAndAlertIdAndEndedAtIsNull(alert.tenantId(), alert.id())
                .ifPresent(event -> {
                    event.setEndedAt(Instant.now());
                    repository.save(event);
                    log.info("Temps d'arret clos (equipement {}, alerte {})",
                            alert.equipmentId(), alert.id());
                });
    }

    @Transactional(readOnly = true)
    public AvailabilityView availability(UUID tenantId, UUID equipmentId,
                                         Instant from, Instant to) {
        Instant windowStart = from != null ? from : Instant.now().minusSeconds(24 * 3600);
        Instant windowEnd = to != null ? to : Instant.now();
        if (windowEnd.isBefore(windowStart)) {
            windowEnd = windowStart;
        }
        List<DowntimeEvent> events = repository.overlapping(tenantId, equipmentId, windowStart, windowEnd);
        long totalMinutes = Math.max(1, java.time.Duration.between(windowStart, windowEnd).toMinutes());
        long downtimeMinutes = 0;
        for (DowntimeEvent event : events) {
            Instant start = event.getStartedAt().isAfter(windowStart) ? event.getStartedAt() : windowStart;
            Instant end = event.getEndedAt() != null && event.getEndedAt().isBefore(windowEnd)
                    ? event.getEndedAt() : windowEnd;
            if (end.isAfter(start)) {
                downtimeMinutes += java.time.Duration.between(start, end).toMinutes();
            }
        }
        long uptimeMinutes = Math.max(0, totalMinutes - downtimeMinutes);
        double availabilityPercent = 100.0 * uptimeMinutes / totalMinutes;
        return new AvailabilityView(equipmentId, windowStart, windowEnd, totalMinutes,
                uptimeMinutes, downtimeMinutes, Math.round(availabilityPercent * 100.0) / 100.0,
                events.size());
    }

    @Transactional(readOnly = true)
    public long downtimeTodayMinutes(UUID tenantId) {
        Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = Instant.now();
        return availability(tenantId, null, todayStart, now).downtimeMinutes();
    }

    @Transactional(readOnly = true)
    public GlobalOeeView oee(UUID tenantId, Instant from, Instant to) {
        Instant windowStart = from != null ? from : Instant.now().minusSeconds(24 * 3600);
        Instant windowEnd = to != null ? to : Instant.now();
        if (windowEnd.isBefore(windowStart)) {
            windowEnd = windowStart;
        }
        List<DowntimeEvent> events = repository.overlapping(tenantId, null, windowStart, windowEnd);
        long totalMinutes = Math.max(1, java.time.Duration.between(windowStart, windowEnd).toMinutes());
        long downtimeMinutes = mergedDowntimeMinutes(events, windowStart, windowEnd);
        long uptimeMinutes = Math.max(0, totalMinutes - downtimeMinutes);
        double availabilityPercent = 100.0 * uptimeMinutes / totalMinutes;
        long completedOrders = workOrderService.countCompletedBetween(tenantId, windowStart, windowEnd);
        long createdOrders = workOrderService.countCreatedBetween(tenantId, windowStart, windowEnd);
        double completionRate = createdOrders > 0
                ? 100.0 * completedOrders / createdOrders : 0.0;
        return new GlobalOeeView(
                windowStart, windowEnd, totalMinutes, uptimeMinutes, downtimeMinutes,
                Math.round(availabilityPercent * 100.0) / 100.0, events.size(),
                completedOrders, createdOrders, Math.round(completionRate * 100.0) / 100.0);
    }

    private long mergedDowntimeMinutes(List<DowntimeEvent> events, Instant windowStart, Instant windowEnd) {
        List<long[]> intervals = new ArrayList<>();
        for (DowntimeEvent event : events) {
            long start = Math.max(event.getStartedAt().toEpochMilli(), windowStart.toEpochMilli());
            long end = event.getEndedAt() != null
                    ? Math.min(event.getEndedAt().toEpochMilli(), windowEnd.toEpochMilli())
                    : windowEnd.toEpochMilli();
            if (end > start) {
                intervals.add(new long[]{start, end});
            }
        }
        intervals.sort(Comparator.comparingLong(a -> a[0]));
        long mergedMillis = 0;
        long cursorStart = -1;
        long cursorEnd = -1;
        for (long[] interval : intervals) {
            if (interval[0] > cursorEnd) {
                if (cursorStart >= 0) {
                    mergedMillis += cursorEnd - cursorStart;
                }
                cursorStart = interval[0];
                cursorEnd = interval[1];
            } else {
                cursorEnd = Math.max(cursorEnd, interval[1]);
            }
        }
        if (cursorStart >= 0) {
            mergedMillis += cursorEnd - cursorStart;
        }
        return mergedMillis / 60_000;
    }
}