package com.telemetryhub.fleet.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSiteRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 255) String address,
        @Size(max = 80) String city,
        @Size(max = 40) String country,
        double latitude,
        double longitude
) {
}