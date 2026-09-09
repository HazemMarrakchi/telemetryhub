package com.telemetryhub.gateway.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@ConfigurationPropertiesScan("com.telemetryhub.gateway.config")
public class GatewayConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return new CorsWebFilter(source);
    }

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
    public RedisRateLimiter globalRateLimiter() {
        return new RedisRateLimiter(60, 120, 1);
    }
}