package com.techstore.tech_store_project.dto;

import jakarta.validation.constraints.*;
import com.techstore.tech_store_project.model.Permiso;
import java.util.Set;

public record RolRequest(
        @NotBlank(message = "El nombre del rol es obligatorio.") @Size(max = 100) String nombre,
        @Size(max = 255) String descripcion,
        @NotNull(message = "La lista de permisos es obligatoria.") Set<@NotNull Permiso> permisos) {
}
