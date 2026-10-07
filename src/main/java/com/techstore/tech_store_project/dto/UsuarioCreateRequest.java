package com.techstore.tech_store_project.dto;

import jakarta.validation.constraints.*;

public record UsuarioCreateRequest(
        @NotBlank(message = "El usuario es obligatorio.") @Size(max = 100) String username,
        @NotBlank(message = "La contraseña es obligatoria.") @Size(min = 12, max = 72) String password,
        @Size(max = 255) String nombreCompleto,
        @Email(message = "El correo no es válido.") @Size(max = 255) String correo,
        @NotNull(message = "El rol es obligatorio.") @Positive Long rol) {
}
