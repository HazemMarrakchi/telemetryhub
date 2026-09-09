package com.telemetryhub.auth.api;

import com.telemetryhub.auth.domain.Invitation;
import com.telemetryhub.auth.domain.Role;
import com.telemetryhub.auth.security.TenantAuthenticationDetails;
import com.telemetryhub.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register-tenant")
    public ResponseEntity<AuthService.AuthResult> registerTenant(
            @Valid @RequestBody RegisterTenantRequest request) {
        AuthService.AuthResult result = authService.tenantRegistration(
                request.tenantName(), request.slug(),
                request.adminEmail(), request.adminFullName(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthService.AuthResult> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(
                request.email(), request.password(), request.deviceName());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthService.AuthResult> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthService.AuthResult result = authService.refresh(
                request.refreshToken(), request.deviceName());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        if (request != null) {
            authService.logout(request.refreshToken());
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke-all")
    public ResponseEntity<Void> revokeAll(Authentication authentication) {
        UUID userId = currentUserId(authentication);
        authService.revokeAllSessions(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Invitation> invite(@Valid @RequestBody InviteUserRequest request,
                                             Authentication authentication) {
        UUID actorId = currentUserId(authentication);
        UUID tenantId = currentTenantId(authentication);
        int validityHours = request.validityHours() > 0 ? request.validityHours() : 72;
        Invitation invitation = authService.inviteUser(
                actorId, tenantId,
                request.email(), request.fullName(),
                Role.valueOf(request.role().toUpperCase()),
                Instant.now().plus(validityHours, ChronoUnit.HOURS));
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation);
    }

    @GetMapping("/invitation/{token}")
    public ResponseEntity<Invitation> getInvitation(@PathVariable String token) {
        return ResponseEntity.ok(authService.getInvitation(token));
    }

    @PostMapping("/invitation/{token}/accept")
    public ResponseEntity<AuthService.AuthResult> acceptInvitation(
            @PathVariable String token, @Valid @RequestBody AcceptInvitationRequest request) {
        AuthService.AuthResult result = authService.acceptInvitation(
                token, request.password(), request.deviceName());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/invitation/{id}/revoke")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> revokeInvitation(@PathVariable UUID id) {
        authService.revokeInvitation(id);
        return ResponseEntity.noContent().build();
    }

    private static UUID currentUserId(Authentication authentication) {
        TenantAuthenticationDetails details =
                (TenantAuthenticationDetails) authentication.getDetails();
        return details.userId();
    }

    private static UUID currentTenantId(Authentication authentication) {
        TenantAuthenticationDetails details =
                (TenantAuthenticationDetails) authentication.getDetails();
        return details.tenantId();
    }
}