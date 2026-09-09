package com.telemetryhub.auth.security;

import java.util.UUID;

public record TenantAuthenticationDetails(UUID tenantId, UUID userId) {
}