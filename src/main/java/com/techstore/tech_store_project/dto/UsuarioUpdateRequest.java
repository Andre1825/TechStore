package com.techstore.tech_store_project.dto;

import jakarta.validation.constraints.*;

public record UsuarioUpdateRequest(
        @Size(max = 255) String nombreCompleto,
        @Email(message = "El correo no es válido.") @Size(max = 255) String correo,
        @Positive Long rol) {
}
