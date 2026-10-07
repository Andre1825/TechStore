package com.techstore.tech_store_project.dto;

import jakarta.validation.constraints.*;

public record CambioPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria.") @Size(max = 72) String passwordActual,
        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(min = 12, max = 72, message = "La nueva contraseña debe tener entre 12 y 72 caracteres.") String passwordNueva) {
}
