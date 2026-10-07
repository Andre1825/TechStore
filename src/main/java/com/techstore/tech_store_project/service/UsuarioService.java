package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Rol;
import com.techstore.tech_store_project.model.Usuario;
import com.techstore.tech_store_project.repository.RolRepository;
import com.techstore.tech_store_project.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.techstore.tech_store_project.dto.UsuarioCreateRequest;
import com.techstore.tech_store_project.dto.UsuarioUpdateRequest;
import com.techstore.tech_store_project.dto.PerfilRequest;
import java.util.Objects;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRevocationService sessionRevocationService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder, SessionRevocationService sessionRevocationService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRevocationService = sessionRevocationService;
    }

    public List<Map<String, Object>> listar() {
        return usuarioRepository.findAll().stream().map(this::toDto).toList();
    }

    // RF-03: Registrar usuario con username/correo únicos y rol asignado
    public Map<String, Object> crear(UsuarioCreateRequest body) {
        String username = body.username().trim();
        String password = body.password();
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Usuario y contraseña son obligatorios.");
        }
        PasswordPolicy.validar(password);
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictoException("El nombre de usuario ya existe.");
        }
        String correo = normalizarCorreo(body.correo());
        if (correo != null && usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("El correo ya está registrado.");
        }
        Rol rol = resolverRol(body.rol());
        if (rol == null) {
            throw new IllegalArgumentException("Debe seleccionar un rol válido.");
        }

        Usuario u = new Usuario();
        u.setUsername(username);
        // RNF-02: Cifrar contraseña con BCrypt antes de guardar
        u.setPassword(passwordEncoder.encode(password));
        u.setNombreCompleto(body.nombreCompleto());
        u.setCorreo(correo);
        u.setRol(rol);
        u.setActivo(true);
        usuarioRepository.save(u);
        return toDto(u);
    }

    @Transactional
    public Map<String, Object> actualizar(Long id, UsuarioUpdateRequest body) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));

        if (body.nombreCompleto() != null) u.setNombreCompleto(body.nombreCompleto());
        if (body.correo() != null) {
            String correo = normalizarCorreo(body.correo());
            if (correo != null && !correo.equals(u.getCorreo()) && usuarioRepository.existsByCorreo(correo)) {
                throw new ConflictoException("El correo ya está registrado.");
            }
            if (!Objects.equals(correo, u.getCorreo())) u.setStockAlertasActivas(false);
            u.setCorreo(correo);
        }
        if (body.rol() != null) {
            Rol rol = resolverRol(body.rol());
            if (rol == null) {
                throw new IllegalArgumentException("Debe seleccionar un rol válido.");
            }
            if (!rol.getId().equals(u.getRol().getId())) sessionRevocationService.revocar(u.getUsername());
            u.setRol(rol);
        }
        usuarioRepository.save(u);
        return toDto(u);
    }

    // RF-02: Bloquear/desbloquear cuenta (el desbloqueo reinicia los intentos fallidos)
    @Transactional
    public Map<String, Object> toggleBloqueo(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));

        boolean bloqueado = !u.isCuentaBloqueada();
        u.setCuentaBloqueada(bloqueado);
        if (bloqueado) sessionRevocationService.revocar(u.getUsername());
        if (!bloqueado) u.setIntentosFallidos(0);
        usuarioRepository.save(u);
        return toDto(u);
    }

    @Transactional
    public void cambiarPassword(String username, String passwordActual, String passwordNueva) {
        Usuario u = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        if (!passwordEncoder.matches(passwordActual, u.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
        PasswordPolicy.validar(passwordNueva);
        if (passwordEncoder.matches(passwordNueva, u.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual.");
        }
        u.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(u);
        sessionRevocationService.revocar(username);
    }

    @Transactional
    public void actualizarPerfil(String username, PerfilRequest body) {
        Usuario user = usuarioRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
        String correo = normalizarCorreo(body.correo());
        if (correo != null && !correo.equals(user.getCorreo()) && usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("El correo ya está registrado.");
        }
        if (!Objects.equals(correo, user.getCorreo())) user.setStockAlertasActivas(false);
        user.setCorreo(correo);
        user.setNombreCompleto(body.nombreCompleto() == null ? null : body.nombreCompleto().trim());
        usuarioRepository.save(user);
    }

    private Rol resolverRol(Long rolId) {
        return rolId == null ? null : rolRepository.findById(rolId).filter(Rol::isActivo).orElse(null);
    }

    private String normalizarCorreo(String correo) {
        return correo == null || correo.isBlank() ? null : correo.trim().toLowerCase(java.util.Locale.ROOT);
    }

    // DTO sin la contraseña (nunca se expone el hash BCrypt)
    public Map<String, Object> toDto(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nombreCompleto", u.getNombreCompleto());
        m.put("correo", u.getCorreo());
        m.put("rol", u.getRol() != null ? u.getRol().getNombre() : null);
        m.put("rolId", u.getRol() != null ? u.getRol().getId() : null);
        m.put("activo", u.isActivo());
        m.put("cuentaBloqueada", u.isCuentaBloqueada());
        m.put("intentosFallidos", u.getIntentosFallidos());
        m.put("ultimoAcceso", u.getUltimoAcceso());
        return m;
    }
}
