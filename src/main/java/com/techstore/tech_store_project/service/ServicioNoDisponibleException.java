package com.techstore.tech_store_project.service;

public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje) { super(mensaje); }
}
