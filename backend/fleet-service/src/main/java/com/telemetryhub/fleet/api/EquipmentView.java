package com.telemetryhub.fleet.api;

import com.telemetryhub.fleet.domain.Equipment;
import com.telemetryhub.fleet.domain.EquipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record EquipmentView(
        UUID id,
        String name,
        String serialNumber,
        ModelView model,
        SiteView site,
        EquipmentStatus status,
        Instant installedAt,
        Instant lastSeenAt,
        Instant nextMaintenanceAt,
        String firmwareVersion,
        String notes
) {
    public static EquipmentView from(Equipment equipment) {
        return new EquipmentView(
                equipment.getId(), equipment.getName(), equipment.getSerialNumber(),
                ModelView.from(equipment.getModel()), SiteView.from(equipment.getSite()),
                equipment.getStatus(), equipment.getInstalledAt(), equipment.getLastSeenAt(),
                equipment.getNextMaintenanceAt(), equipment.getFirmwareVersion(),
                equipment.getNotes());
    }
}