package com.telemetryhub.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank @Size(min = 10, max = 100) String password,
        String deviceName
) {
}