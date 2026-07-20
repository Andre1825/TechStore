package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API de autenticación para el frontend React (SPA).
 * Usa la misma sesión HTTP de Spring Security que el resto de la aplicación.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AuthService authService;

    public AuthApiController(AuthenticationManager authenticationManager,
                             SecurityContextRepository securityContextRepository,
                             AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.authService = authService;
    }

    // RF-01: Validar credenciales contra la BD (mismo AuthenticationManager que el login web)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            return ResponseEntity.ok(authService.buildMe(auth.getName()));
        } catch (LockedException e) {
            // RF-02: Cuenta bloqueada tras 3 intentos fallidos
            return ResponseEntity.status(401)
                    .body(Map.of("error", "locked", "mensaje", "Cuenta bloqueada por múltiples intentos fallidos."));
        } catch (DisabledException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "disabled", "mensaje", "La cuenta está desactivada."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "bad_credentials", "mensaje", "Usuario o contraseña incorrectos."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // Usuario autenticado actual (para restaurar sesión al recargar la SPA)
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401).body(Map.of("error", "no_autenticado"));
        }
        return ResponseEntity.ok(authService.buildMe(auth.getName()));
    }
}
