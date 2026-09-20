package com.telemetryhub.gateway.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewayJwtService jwtService;

    public JwtAuthenticationWebFilter(GatewayJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        boolean isPublic = path.startsWith("/actuator")
                || path.equals("/api/v1/auth/register-tenant")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/auth/invitation/")
                || "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name());
        if (isPublic) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        try {
            DecodedJWT decoded = jwtService.verify(header.substring(BEARER_PREFIX.length()));
            if (jwtService.isRefreshToken(decoded)) {
                return unauthorized(exchange);
            }

            ServerWebExchange mutated = exchange.mutate()
                    .request(builder -> builder
                            .header("X-Tenant-Id", decoded.getClaim("tid").asString())
                            .header("X-User-Id", decoded.getClaim("uid").asString())
                            .header("X-User-Role", decoded.getClaim("role").asString()))
                    .build();
            return chain.filter(mutated);
        } catch (InvalidTokenException ex) {
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory()
                        .wrap("{\"code\":\"invalid_token\",\"message\":\"Authentification requise\"}"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
}