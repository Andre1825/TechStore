package com.techstore.tech_store_project.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductoRequest(
        @Size(max = 50, message = "El SKU admite hasta 50 caracteres.") String sku,
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 150, message = "El nombre admite hasta 150 caracteres.") String nombre,
        @Size(max = 255, message = "La descripción admite hasta 255 caracteres.") String descripcion,
        @NotNull(message = "El precio es obligatorio.")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "El precio admite 10 dígitos enteros y 2 decimales.") BigDecimal precio,
        @NotNull(message = "El stock mínimo es obligatorio.")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo.") Integer stockMinimo,
        @NotNull(message = "La categoría es obligatoria.")
        @Positive(message = "La categoría debe ser válida.") Long categoriaId,
        @Positive(message = "La marca debe ser válida.") Long marcaId) {
}
