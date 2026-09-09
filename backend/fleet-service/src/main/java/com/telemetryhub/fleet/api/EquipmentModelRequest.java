package com.telemetryhub.fleet.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentModelRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String manufacturer,
        @Size(max = 500) String description,
        @NotBlank @Size(max = 40) String category
) {
}