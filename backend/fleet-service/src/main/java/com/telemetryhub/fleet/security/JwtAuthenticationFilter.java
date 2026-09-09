package com.telemetryhub.fleet.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            DecodedJWT decoded = jwtService.verify(header.substring(BEARER_PREFIX.length()));
            if (jwtService.isRefreshToken(decoded)) {
                filterChain.doFilter(request, response);
                return;
            }

            String role = decoded.getClaim("role").asString();
            UUID tenantId = UUID.fromString(decoded.getClaim("tid").asString());
            UUID userId = UUID.fromString(decoded.getClaim("uid").asString());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            managerId(tenantId, userId), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));

            TenantContext.set(new TenantContext(tenantId, userId));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (InvalidTokenException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String managerId(UUID tenantId, UUID userId) {
        return tenantId.toString() + "|" + userId.toString();
    }
}