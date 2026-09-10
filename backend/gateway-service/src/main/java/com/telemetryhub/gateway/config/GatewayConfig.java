package com.telemetryhub.gateway.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
@ConfigurationPropertiesScan("com.telemetryhub.gateway.config")
public class GatewayConfig {

    @Bean
    public KeyResolver tenantKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getHeaders().getFirst("X-Tenant-Id"))
                .defaultIfEmpty(exchange.getRequest().getRemoteAddress() == null
                        ? "unknown" : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    @Bean
    public RedisRateLimiter telemetryRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }

    @Bean
    @Primary
    public RedisRateLimiter globalRateLimiter() {
        return new RedisRateLimiter(60, 120, 1);
    }
}