package com.telemetryhub.maintenance.api;

import jakarta.validation.constraints.Size;

public record CompleteWorkOrderRequest(
        @Size(max = 2000, message = "Les notes ne doivent pas dépasser 2000 caractères")
        String completionNotes
) {
}