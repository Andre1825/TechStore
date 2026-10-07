package com.techstore.tech_store_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.*;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // RNF-02: Cifrar todas las contraseñas de usuarios utilizando el algoritmo hash BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expone el AuthenticationManager para el login JSON de la SPA React (/api/auth/login)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Repositorio de contexto en sesión HTTP: la SPA comparte la misma sesión que el login web
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry registry,
                                                                      CsrfTokenRepository csrfRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new org.springframework.security.web.csrf.CsrfAuthenticationStrategy(csrfRepository),
                new RegisterSessionAuthenticationStrategy(registry)));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SecurityContextRepository securityContextRepository,
                                           CsrfTokenRepository csrfTokenRepository,
                                           SessionAuthenticationStrategy sessionAuthenticationStrategy,
                                           SessionRegistry sessionRegistry) throws Exception {
        http
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            .sessionManagement(sm -> sm.sessionAuthenticationStrategy(sessionAuthenticationStrategy))
            .addFilterAt(new ConcurrentSessionFilter(sessionRegistry, event -> {
                event.getResponse().setStatus(401);
                event.getResponse().setContentType("application/json;charset=UTF-8");
                event.getResponse().getWriter().write("{\"mensaje\":\"La sesión fue revocada. Vuelve a iniciar sesión.\"}");
            }), ConcurrentSessionFilter.class)
            // RF-03: Autorización por PERMISO de módulo (RBAC). Las authorities son los permisos del rol.
            .authorizeHttpRequests(auth -> auth
                // Cascaron publico de la SPA React (HTML/JS/CSS/imagenes) — no contiene datos sensibles
                .requestMatchers("/", "/index.html", "/assets/**",
                        "/css/**", "/js/**", "/img/**",
                        "/favicon.ico", "/favicon.svg", "/vite.svg").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                .requestMatchers("/api/auth/stock-alerts/**", "/api/auth/stock-alerts").hasAuthority("GESTIONAR_USUARIOS")
                .requestMatchers("/api/auth/**").authenticated()
                // Lecturas de catálogo: cualquier usuario autenticado (necesarias para registrar salidas, etc.)
                .requestMatchers(HttpMethod.GET, "/api/categorias/**", "/api/marcas/**", "/api/productos/**", "/api/roles/activos").authenticated()
                // Escritura del catálogo, gestionada por el permiso de cada módulo
                .requestMatchers("/api/categorias/**").hasAuthority("GESTIONAR_CATEGORIAS")
                .requestMatchers("/api/marcas/**").hasAuthority("GESTIONAR_MARCAS")
                .requestMatchers("/api/productos/**").hasAuthority("GESTIONAR_PRODUCTOS")
                // Operaciones de inventario
                .requestMatchers("/api/entradas/**").hasAuthority("REGISTRAR_ENTRADAS")
                .requestMatchers("/api/salidas/**").hasAuthority("REGISTRAR_SALIDAS")
                .requestMatchers("/api/movimientos/**").hasAuthority("VER_MOVIMIENTOS")
                // Dashboard y administración
                .requestMatchers("/api/dashboard/**").hasAuthority("VER_DASHBOARD")
                .requestMatchers("/api/usuarios/**").hasAuthority("GESTIONAR_USUARIOS")
                .requestMatchers("/api/roles/**").hasAuthority("GESTIONAR_ROLES")
                .requestMatchers("/export/productos.xlsx").hasAuthority("GESTIONAR_PRODUCTOS")
                .requestMatchers("/export/movimientos.xlsx").hasAuthority("VER_MOVIMIENTOS")
                .requestMatchers("/export/**").denyAll()
                .requestMatchers("/actuator/**").hasAuthority("GESTIONAR_USUARIOS")
                // Cualquier otra ruta de API exige sesión (candado por defecto de la API)
                .requestMatchers("/api/**").authenticated()
                // El resto son rutas del cliente (React Router) → servir la SPA públicamente
                .anyRequest().permitAll()
            )
            // Autenticación vía /api/auth/login (AuthApiController). Sin formLogin ni logout web.
            .exceptionHandling(ex -> ex
                // Sin sesión → 401 JSON (la SPA redirige a su propia pantalla de login)
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                // Sin permisos → 403 (la SPA muestra su pantalla de acceso denegado)
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    if (e instanceof org.springframework.security.web.csrf.CsrfException) {
                        response.getWriter().write("{\"error\":\"csrf\",\"mensaje\":\"La protección de la sesión debe renovarse.\"}");
                    } else {
                        response.getWriter().write("{\"mensaje\":\"No tienes permiso para realizar esta operación.\"}");
                    }
                })
            );

        return http.build();
    }
}
