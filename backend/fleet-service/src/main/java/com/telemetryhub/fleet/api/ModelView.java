package com.telemetryhub.fleet.api;

import com.telemetryhub.fleet.domain.EquipmentModel;

import java.time.Instant;
import java.util.UUID;

public record ModelView(
        UUID id,
        String name,
        String manufacturer,
        String description,
        String category,
        boolean active,
        Instant createdAt
) {
    public static ModelView from(EquipmentModel model) {
        return new ModelView(
                model.getId(), model.getName(), model.getManufacturer(),
                model.getDescription(), model.getCategory(),
                model.isActive(), model.getCreatedAt());
    }
}