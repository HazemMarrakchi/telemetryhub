package com.telemetryhub.gateway;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.telemetryhub.gateway.config.JwtProperties;
import com.telemetryhub.gateway.security.GatewayJwtService;
import com.telemetryhub.gateway.security.JwtAuthenticationWebFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationWebFilterTest {

    private static final String SECRET =
            "TEST_SECRET_KEY_THAT_IS_AT_LEAST_32_BYTES_LONG_FOR_TESTS";

    private JwtAuthenticationWebFilter filter;
    private boolean chainCalled;

    @BeforeEach
    void setUp() {
        GatewayJwtService jwtService = new GatewayJwtService(
                new JwtProperties("telemetryhub-auth", SECRET));
        filter = new JwtAuthenticationWebFilter(jwtService);
        chainCalled = false;
    }

    private String validToken() {
        return JWT.create()
                .withIssuer("telemetryhub-auth")
                .withSubject("admin@acme.com")
                .withClaim("uid", UUID.randomUUID().toString())
                .withClaim("tid", UUID.randomUUID().toString())
                .withClaim("role", "TENANT_ADMIN")
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(900))
                .sign(Algorithm.HMAC256(SECRET));
    }

    private WebFilterChain chain() {
        return exchange -> {
            chainCalled = true;
            return Mono.empty();
        };
    }

    @Test
    void allowsAuthenticatedRequestAndAddsHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/equipments")
                .header("Authorization", "Bearer " + validToken())
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain()).block();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-Tenant-Id")).isNotBlank();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id")).isNotBlank();
        assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("TENANT_ADMIN");
    }

    @Test
    void rejectsMissingToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/equipments").build());

        filter.filter(exchange, chain()).block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void rejectsInvalidToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/equipments")
                        .header("Authorization", "Bearer invalid.token.value")
                        .build());

        filter.filter(exchange, chain()).block();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void allowsActuatorWithoutAuthentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        filter.filter(exchange, chain()).block();

        assertThat(chainCalled).isTrue();
    }
}