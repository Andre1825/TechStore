package com.techstore.tech_store_project.model;

/**
 * RF-03: Catálogo fijo de permisos del sistema (uno por módulo funcional).
 * Cada valor se usa como "authority" de Spring Security (ver SecurityConfig y UserDetailsServiceImpl).
 */
public enum Permiso {
    VER_DASHBOARD("Ver dashboard"),
    GESTIONAR_PRODUCTOS("Gestionar productos"),
    GESTIONAR_CATEGORIAS("Gestionar categorías"),
    GESTIONAR_MARCAS("Gestionar marcas"),
    REGISTRAR_ENTRADAS("Registrar entradas"),
    REGISTRAR_SALIDAS("Registrar salidas"),
    VER_MOVIMIENTOS("Ver movimientos (Kardex)"),
    GESTIONAR_USUARIOS("Gestionar usuarios"),
    GESTIONAR_ROLES("Gestionar roles");

    private final String etiqueta;

    Permiso(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
