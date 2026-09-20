package com.telemetryhub.maintenance.security;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<Holder> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId, UUID userId) {
        CURRENT.set(new Holder(tenantId, userId));
    }

    public static UUID tenantId() {
        Holder holder = CURRENT.get();
        return holder != null ? holder.tenantId : null;
    }

    public static UUID userId() {
        Holder holder = CURRENT.get();
        return holder != null ? holder.userId : null;
    }

    public static void clear() {
        CURRENT.remove();
    }

    private record Holder(UUID tenantId, UUID userId) {
    }
}