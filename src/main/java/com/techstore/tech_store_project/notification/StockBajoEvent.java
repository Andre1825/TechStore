package com.techstore.tech_store_project.notification;

public record StockBajoEvent(Long productoId, String sku, String nombre, int stockAnterior,
                             int stockActual, int stockMinimo, Long movimientoId) {
}
