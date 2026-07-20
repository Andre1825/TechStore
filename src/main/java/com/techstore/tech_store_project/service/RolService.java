package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Permiso;
import com.techstore.tech_store_project.model.Rol;
import com.techstore.tech_store_project.model.Usuario;
import com.techstore.tech_store_project.repository.RolRepository;
import com.techstore.tech_store_project.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

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

    public RolService(RolRepository rolRepository, UsuarioRepository usuarioRepository) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
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

    public Map<String, Object> crear(Map<String, Object> body) {
        String nombre = String.valueOf(body.getOrDefault("nombre", "")).trim();
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio.");
        }
        if (rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe un rol con ese nombre.");
        }
        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(strOrNull(body.get("descripcion")));
        rol.setPermisos(parsePermisos(body.get("permisos")));
        rol.setActivo(true);
        rolRepository.save(rol);
        return toDto(rol, 0L);
    }

    public Map<String, Object> actualizar(Long id, Map<String, Object> body) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + id));

        String nombre = String.valueOf(body.getOrDefault("nombre", rol.getNombre())).trim();
        if (!rol.getNombre().equalsIgnoreCase(nombre) && rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException("Ya existe un rol con ese nombre.");
        }
        rol.setNombre(nombre);
        if (body.containsKey("descripcion")) rol.setDescripcion(strOrNull(body.get("descripcion")));
        if (body.containsKey("permisos")) rol.setPermisos(parsePermisos(body.get("permisos")));
        rolRepository.save(rol);
        return toDto(rol, null);
    }

    // Convierte la lista JSON de claves en un conjunto de Permiso, ignorando valores inválidos
    private Set<Permiso> parsePermisos(Object raw) {
        Set<Permiso> permisos = new TreeSet<>();
        if (raw instanceof List<?> lista) {
            for (Object o : lista) {
                try {
                    permisos.add(Permiso.valueOf(String.valueOf(o)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return permisos;
    }

    private String strOrNull(Object o) {
        return o == null ? null : String.valueOf(o);
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
