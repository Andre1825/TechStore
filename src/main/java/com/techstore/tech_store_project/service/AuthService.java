package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Usuario;
import com.techstore.tech_store_project.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final StockAlertService stockAlertService;

    public AuthService(UsuarioRepository usuarioRepository, StockAlertService stockAlertService) {
        this.usuarioRepository = usuarioRepository;
        this.stockAlertService = stockAlertService;
    }

    // RF-03: Devuelve el rol (nombre) y sus permisos, para que la SPA controle la UI
    public Map<String, Object> buildMe(String username) {
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("username", username);
        Usuario u = usuarioRepository.findByUsername(username).orElse(null);
        if (u != null) {
            me.put("rol", u.getRol() != null ? u.getRol().getNombre() : null);
            me.put("rolId", u.getRol() != null ? u.getRol().getId() : null);
            me.put("permisos", u.getRol() != null
                    ? u.getRol().getPermisos().stream().map(Enum::name).sorted().toList()
                    : List.of());
            me.put("nombreCompleto", u.getNombreCompleto());
            me.put("correo", u.getCorreo());
            me.put("stockAlertasActivas", u.isStockAlertasActivas());
            me.put("avisosStockDisponibles", stockAlertService.disponible());
            me.put("ultimoAcceso", u.getUltimoAcceso());
        }
        return me;
    }
}
