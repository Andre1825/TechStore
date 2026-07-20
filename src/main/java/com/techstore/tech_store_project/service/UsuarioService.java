package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Rol;
import com.techstore.tech_store_project.model.Usuario;
import com.techstore.tech_store_project.repository.RolRepository;
import com.techstore.tech_store_project.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Map<String, Object>> listar() {
        return usuarioRepository.findAll().stream().map(this::toDto).toList();
    }

    // RF-03: Registrar usuario con username/correo únicos y rol asignado
    public Map<String, Object> crear(Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Usuario y contraseña son obligatorios.");
        }
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictoException("El nombre de usuario ya existe.");
        }
        String correo = body.getOrDefault("correo", "");
        if (!correo.isBlank() && usuarioRepository.existsByCorreo(correo)) {
            throw new ConflictoException("El correo ya está registrado.");
        }
        Rol rol = resolverRol(body.get("rol"));
        if (rol == null) {
            throw new IllegalArgumentException("Debe seleccionar un rol válido.");
        }

        Usuario u = new Usuario();
        u.setUsername(username);
        // RNF-02: Cifrar contraseña con BCrypt antes de guardar
        u.setPassword(passwordEncoder.encode(password));
        u.setNombreCompleto(body.getOrDefault("nombreCompleto", ""));
        u.setCorreo(correo);
        u.setRol(rol);
        u.setActivo(true);
        usuarioRepository.save(u);
        return toDto(u);
    }

    public Map<String, Object> actualizar(Long id, Map<String, String> body) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));

        u.setNombreCompleto(body.getOrDefault("nombreCompleto", u.getNombreCompleto()));
        u.setCorreo(body.getOrDefault("correo", u.getCorreo()));
        if (body.get("rol") != null) {
            Rol rol = resolverRol(body.get("rol"));
            if (rol == null) {
                throw new IllegalArgumentException("Debe seleccionar un rol válido.");
            }
            u.setRol(rol);
        }
        usuarioRepository.save(u);
        return toDto(u);
    }

    // RF-02: Bloquear/desbloquear cuenta (el desbloqueo reinicia los intentos fallidos)
    public Map<String, Object> toggleBloqueo(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));

        boolean bloqueado = !u.isCuentaBloqueada();
        u.setCuentaBloqueada(bloqueado);
        if (!bloqueado) u.setIntentosFallidos(0);
        usuarioRepository.save(u);
        return toDto(u);
    }

    private Rol resolverRol(String rolId) {
        if (rolId == null || rolId.isBlank()) return null;
        try {
            return rolRepository.findById(Long.valueOf(rolId.trim())).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
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
