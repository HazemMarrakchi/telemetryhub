package com.telemetryhub.auth.service;

import com.telemetryhub.auth.config.JwtProperties;
import com.telemetryhub.auth.domain.Invitation;
import com.telemetryhub.auth.domain.InvitationStatus;
import com.telemetryhub.auth.domain.RefreshToken;
import com.telemetryhub.auth.domain.Role;
import com.telemetryhub.auth.domain.Tenant;
import com.telemetryhub.auth.domain.User;
import com.telemetryhub.auth.persistence.InvitationRepository;
import com.telemetryhub.auth.persistence.TenantRepository;
import com.telemetryhub.auth.persistence.UserRepository;
import com.telemetryhub.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String REGISTRATIONS_EMAIL_PATTERN = ".*";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       InvitationRepository invitationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenService tokenService,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.invitationRepository = invitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResult tenantRegistration(String tenantName, String slug,
                                         String adminEmail, String adminFullName, String password) {
        String normalizedEmail = adminEmail.toLowerCase(Locale.ROOT).trim();
        String normalizedSlug = slug.toLowerCase(Locale.ROOT).trim();

        if (tenantRepository.existsBySlug(normalizedSlug)) {
            throw new ConflictException("Le slug '" + normalizedSlug + "' est déjà utilisé");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Un compte existe déjà avec cet email");
        }
        if (password.length() < 10) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 10 caractères");
        }

        Tenant tenant = new Tenant(tenantName, normalizedSlug, "STANDARD");
        tenant = tenantRepository.save(tenant);

        User admin = new User(tenant, normalizedEmail, adminFullName,
                passwordEncoder.encode(password), Role.TENANT_ADMIN);
        admin.setEmailVerified(true);
        admin = userRepository.save(admin);

        String accessToken = jwtService.issueAccessToken(admin);
        RefreshToken refresh = issueRefresh(admin, "registration");
        return new AuthResult(accessToken, refresh.getJti(),
                new UserDto(admin.getId(), admin.getEmail(), admin.getFullName(),
                        admin.getRole(), admin.getTenant().getId(), admin.getTenant().getSlug()));
    }

    @Transactional
    public AuthResult login(String email, String password, String deviceName) {
        User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT).trim())
                .orElseThrow(() -> new InvalidCredentialsException("Email ou mot de passe incorrect"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Compte désactivé");
        }
        if (user.isLocked()) {
            throw new InvalidCredentialsException("Compte verrouillé temporairement");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.recordFailedLogin(MAX_FAILED_ATTEMPTS);
            userRepository.save(user);
            throw new InvalidCredentialsException("Email ou mot de passe incorrect");
        }

        user.markLogin();
        userRepository.save(user);

        String accessToken = jwtService.issueAccessToken(user);
        RefreshToken refresh = issueRefresh(user, deviceName);
        return new AuthResult(accessToken, refresh.getJti(),
                new UserDto(user.getId(), user.getEmail(), user.getFullName(),
                        user.getRole(), user.getTenant().getId(), user.getTenant().getSlug()));
    }

    @Transactional
    public AuthResult refresh(String refreshJti, String deviceName) {
        RefreshToken rotated = tokenService.rotateRefreshToken(refreshJti);
        User user = userRepository.findById(rotated.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("Utilisateur introuvable"));
        if (!user.isActive()) {
            throw new InvalidCredentialsException("Compte désactivé");
        }

        String accessToken = jwtService.issueAccessToken(user);
        RefreshToken newRefresh = issueRefresh(user, deviceName == null ? rotated.getDeviceName() : deviceName);
        return new AuthResult(accessToken, newRefresh.getJti(),
                new UserDto(user.getId(), user.getEmail(), user.getFullName(),
                        user.getRole(), user.getTenant().getId(), user.getTenant().getSlug()));
    }

    @Transactional
    public void logout(String refreshJti) {
        if (refreshJti != null) {
            tokenService.revokeRefreshToken(refreshJti);
        }
    }

    private RefreshToken issueRefresh(User user, String deviceName) {
        Instant expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        String jti = UUID.randomUUID().toString();
        return tokenService.storeRefreshToken(jti, user.getId(), user.getTenant().getId(), deviceName);
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
        tokenService.revokeAllForUser(userId);
    }

    @Transactional
    public Invitation inviteUser(UUID actorId, UUID tenantId, String email, String fullName,
                                 Role role, Instant expiresAt) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT).trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Un compte existe déjà avec cet email");
        }
        boolean hasPending = invitationRepository
                .existsByEmailAndTenantIdAndStatus(normalizedEmail, tenantId, InvitationStatus.PENDING);
        if (hasPending) {
            throw new ConflictException("Une invitation en attente existe déjà pour cet email");
        }
        Invitation invitation = new Invitation(
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""),
                tenantId, normalizedEmail, fullName, role, expiresAt, actorId);
        return invitationRepository.save(invitation);
    }

    @Transactional
    public Invitation getInvitation(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Invitation introuvable ou invalide"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvalidCredentialsException("Invitation déjà utilisée");
        }
        if (invitation.isExpired()) {
            throw new InvalidCredentialsException("Invitation expirée");
        }
        return invitation;
    }

    @Transactional
    public AuthResult acceptInvitation(String token, String password, String deviceName) {
        Invitation invitation = getInvitation(token);
        Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));
        User user = new User(tenant, invitation.getEmail(), invitation.getFullName(),
                passwordEncoder.encode(password), invitation.getRole());
        user.setEmailVerified(true);
        user = userRepository.save(user);
        invitation.markAccepted();
        invitationRepository.save(invitation);

        String accessToken = jwtService.issueAccessToken(user);
        RefreshToken refresh = issueRefresh(user, deviceName);
        return new AuthResult(accessToken, refresh.getJti(),
                new UserDto(user.getId(), user.getEmail(), user.getFullName(),
                        user.getRole(), user.getTenant().getId(), user.getTenant().getSlug()));
    }

    @Transactional
    public void revokeInvitation(UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation introuvable"));
        invitation.markRevoked();
        invitationRepository.save(invitation);
    }

    public record AuthResult(String accessToken, String refreshToken, UserDto user) {
    }

    public record UserDto(UUID id, String email, String fullName, Role role,
                          UUID tenantId, String tenantSlug) {
    }
}