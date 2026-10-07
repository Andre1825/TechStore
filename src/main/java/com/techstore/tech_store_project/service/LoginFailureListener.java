package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

@Component
public class LoginFailureListener {

    private final UsuarioRepository usuarioRepository;
    private final SessionRevocationService sessionRevocationService;

    public LoginFailureListener(UsuarioRepository usuarioRepository, SessionRevocationService sessionRevocationService) {
        this.usuarioRepository = usuarioRepository;
        this.sessionRevocationService = sessionRevocationService;
    }

    // RF-02: Bloqueo por intentos fallidos - Bloquear cuenta tras 3 intentos fallidos
    @EventListener
    @Transactional
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();

        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            if (!usuario.isCuentaBloqueada()) {
                usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
                // RF-02: Si alcanza 3 intentos fallidos, bloquear cuenta
                if (usuario.getIntentosFallidos() >= 3) {
                    usuario.setCuentaBloqueada(true);
                    sessionRevocationService.revocar(username);
                }
                usuarioRepository.save(usuario);
            }
        });
    }
}
