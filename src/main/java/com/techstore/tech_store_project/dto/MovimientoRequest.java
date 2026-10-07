package com.techstore.tech_store_project.dto;

import jakarta.validation.constraints.*;

public record MovimientoRequest(
        @NotNull(message = "El producto es obligatorio.")
        @Positive(message = "El producto debe ser válido.") Long productoId,
        @NotNull(message = "La cantidad es obligatoria.")
        @Positive(message = "La cantidad debe ser un entero mayor que cero.") Integer cantidad,
        @Size(max = 100, message = "El documento admite hasta 100 caracteres.") String documentoRef,
        @Size(max = 255, message = "La observación admite hasta 255 caracteres.") String observacion) {
}
