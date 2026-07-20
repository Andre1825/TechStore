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
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SecurityContextRepository securityContextRepository) throws Exception {
        http
            // Backend solo-API: sesión + JSON, sin formularios HTML. No requiere token CSRF.
            .csrf(csrf -> csrf.disable())
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            // RF-03: Autorización por PERMISO de módulo (RBAC). Las authorities son los permisos del rol.
            .authorizeHttpRequests(auth -> auth
                // Cascaron publico de la SPA React (HTML/JS/CSS/imagenes) — no contiene datos sensibles
                .requestMatchers("/", "/index.html", "/assets/**",
                        "/css/**", "/js/**", "/img/**",
                        "/favicon.ico", "/favicon.svg", "/vite.svg").permitAll()
                // API de autenticación de la SPA (login/logout/me)
                .requestMatchers("/api/auth/**").permitAll()
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
                // Exportaciones y actuator requieren sesión
                .requestMatchers("/export/**", "/actuator/**").authenticated()
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
                .accessDeniedHandler((request, response, e) -> response.sendError(HttpStatus.FORBIDDEN.value()))
            );

        return http.build();
    }
}
