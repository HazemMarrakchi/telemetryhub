package com.telemetryhub.fleet.api;

import com.telemetryhub.fleet.domain.Site;

import java.time.Instant;
import java.util.UUID;

public record SiteView(
        UUID id,
        String name,
        String address,
        String city,
        String country,
        double latitude,
        double longitude,
        boolean active,
        Instant createdAt
) {
    public static SiteView from(Site site) {
        return new SiteView(
                site.getId(), site.getName(), site.getAddress(), site.getCity(),
                site.getCountry(), site.getLatitude(), site.getLongitude(),
                site.isActive(), site.getCreatedAt());
    }
}