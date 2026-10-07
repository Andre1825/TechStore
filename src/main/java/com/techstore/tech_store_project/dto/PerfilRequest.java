package com.techstore.tech_store_project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        @Size(max = 255) String nombreCompleto,
        @Email(message = "El correo no es válido.") @Size(max = 255) String correo) {
}
