package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class LoginSuccessListener {

    private final UsuarioRepository usuarioRepository;

    public LoginSuccessListener(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // RF-01: Login y validación + RF-02: Resetear intentos fallidos tras acceso exitoso
    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();

        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            // RF-02: Resetear contador de intentos fallidos al ingresar correctamente
            if (usuario.getIntentosFallidos() > 0) {
                usuario.setIntentosFallidos(0);
            }
            // RF-01: Registrar último acceso del usuario
            usuario.setUltimoAcceso(LocalDateTime.now(ZoneId.of("America/Lima")));
            usuarioRepository.save(usuario);
        });
    }
}
