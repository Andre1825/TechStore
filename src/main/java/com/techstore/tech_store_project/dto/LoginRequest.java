package com.techstore.tech_store_project.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio.") @Size(max = 255) String username,
        @NotBlank(message = "La contraseña es obligatoria.") @Size(max = 72) String password) {
}
