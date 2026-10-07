package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.service.AuthService;
import com.techstore.tech_store_project.service.UsuarioService;
import com.techstore.tech_store_project.dto.LoginRequest;
import com.techstore.tech_store_project.dto.CambioPasswordRequest;
import com.techstore.tech_store_project.dto.PerfilRequest;
import com.techstore.tech_store_project.service.StockAlertService;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
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
    private final UsuarioService usuarioService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final CsrfTokenRepository csrfTokenRepository;
    private final StockAlertService stockAlertService;

    public AuthApiController(AuthenticationManager authenticationManager,
                             SecurityContextRepository securityContextRepository,
                             AuthService authService, UsuarioService usuarioService,
                             SessionAuthenticationStrategy sessionAuthenticationStrategy,
                             CsrfTokenRepository csrfTokenRepository, StockAlertService stockAlertService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.csrfTokenRepository = csrfTokenRepository;
        this.stockAlertService = stockAlertService;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    // RF-01: Validar credenciales contra la BD (mismo AuthenticationManager que el login web)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        String username = body.username().trim();
        String password = body.password();
        try {
            Authentication auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));

            // Rotar el identificador de sesión, renovar CSRF y registrar la sesión para revocarla.
            sessionAuthenticationStrategy.onAuthentication(auth, request, response);

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
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(null, request, response);
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/password")
    public ResponseEntity<?> cambiarPassword(@Valid @RequestBody CambioPasswordRequest body,
                                            Authentication auth, HttpServletRequest request) {
        usuarioService.cambiarPassword(auth.getName(), body.passwordActual(), body.passwordNueva());
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada. Vuelve a iniciar sesión."));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> actualizarPerfil(@Valid @RequestBody PerfilRequest body, Authentication auth) {
        usuarioService.actualizarPerfil(auth.getName(), body);
        return ResponseEntity.ok(authService.buildMe(auth.getName()));
    }

    @PostMapping("/stock-alerts/subscribe")
    public ResponseEntity<?> activarAvisos(Authentication auth) {
        stockAlertService.activar(auth.getName());
        return ResponseEntity.ok(Map.of("mensaje", "Solicitud registrada. Confirma la suscripción en el correo de Amazon SNS si aún no lo has hecho."));
    }

    @DeleteMapping("/stock-alerts")
    public ResponseEntity<?> desactivarAvisos(Authentication auth) {
        stockAlertService.desactivar(auth.getName());
        return ResponseEntity.ok(Map.of("mensaje", "Avisos de stock desactivados para esta cuenta."));
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
