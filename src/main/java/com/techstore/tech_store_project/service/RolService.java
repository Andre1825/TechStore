package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Permiso;
import com.techstore.tech_store_project.model.Rol;
import com.techstore.tech_store_project.model.Usuario;
import com.techstore.tech_store_project.repository.RolRepository;
import com.techstore.tech_store_project.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.techstore.tech_store_project.dto.RolRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final SessionRevocationService sessionRevocationService;

    public RolService(RolRepository rolRepository, UsuarioRepository usuarioRepository,
                      SessionRevocationService sessionRevocationService) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.sessionRevocationService = sessionRevocationService;
    }

    // Catálogo fijo de permisos del sistema (para pintar los checkboxes en el frontend)
    public List<Map<String, String>> permisos() {
        List<Map<String, String>> lista = new ArrayList<>();
        for (Permiso p : Permiso.values()) {
            lista.add(Map.of("clave", p.name(), "etiqueta", p.getEtiqueta()));
        }
        return lista;
    }

    public List<Map<String, Object>> listar() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return rolRepository.findAll().stream().map(rol -> {
            long count = usuarios.stream()
                    .filter(u -> u.getRol() != null && u.getRol().getId().equals(rol.getId()))
                    .count();
            return toDto(rol, count);
        }).toList();
    }

    public List<Map<String, Object>> activos() {
        return rolRepository.findByActivoTrue().stream()
                .map(r -> toDto(r, null)).toList();
    }

    public Map<String, Object> crear(RolRequest body) {
        String nombre = body.nombre().trim();
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio.");
        }
        if (rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe un rol con ese nombre.");
        }
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(body.descripcion());
        rol.setPermisos(new TreeSet<>(body.permisos()));
        rol.setActivo(true);
        rolRepository.save(rol);
        return toDto(rol, 0L);
    }

    @Transactional
    public Map<String, Object> actualizar(Long id, RolRequest body) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));

        String nombre = body.nombre().trim();
        if (!rol.getNombre().equalsIgnoreCase(nombre) && rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe un rol con ese nombre.");
        }
        rol.setNombre(nombre);
        rol.setDescripcion(body.descripcion());
        rol.setPermisos(new TreeSet<>(body.permisos()));
        rolRepository.save(rol);
        usuarioRepository.findByRolId(id).forEach(u -> sessionRevocationService.revocar(u.getUsername()));
        return toDto(rol, null);
    }

    private Map<String, Object> toDto(Rol r, Long usuarios) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("nombre", r.getNombre());
        m.put("descripcion", r.getDescripcion());
        m.put("permisos", r.getPermisos().stream().map(Enum::name).sorted().toList());
        m.put("activo", r.isActivo());
        if (usuarios != null) m.put("usuarios", usuarios);
        return m;
    }
}
