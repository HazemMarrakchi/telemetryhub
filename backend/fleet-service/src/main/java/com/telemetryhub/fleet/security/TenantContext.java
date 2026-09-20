package com.telemetryhub.fleet.security;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantContext {

    public static final String ATTR = "TELEMETRYHUB_TENANT_ATTR";
    private static final ThreadLocal<TenantContext> CURRENT = new ThreadLocal<>();

    private UUID tenantId;
    private UUID userId;

    public TenantContext() {
    }

    public TenantContext(UUID tenantId, UUID userId) {
        this.tenantId = tenantId;
        this.userId = userId;
    }

    public static void set(TenantContext ctx) {
        CURRENT.set(ctx);
    }

    public static TenantContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }
}