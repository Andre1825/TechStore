package com.techstore.tech_store_project.service;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() { }

    public static void validar(String password) {
        if (password == null || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8.");
        }
    }
}
