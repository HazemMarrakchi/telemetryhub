package com.telemetryhub.auth.api;

import com.telemetryhub.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
        @NotBlank @Size(max = 120) String tenantName,
        @NotBlank @Size(min = 3, max = 80) String slug,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(max = 255) String adminFullName,
        @NotBlank @Size(min = 10, max = 100) String password
) {
}