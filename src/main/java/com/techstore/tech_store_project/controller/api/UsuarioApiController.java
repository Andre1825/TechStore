package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RF-03: API REST de usuarios para el frontend React.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioApiController {

    private final UsuarioService usuarioService;

    public UsuarioApiController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return usuarioService.listar();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(usuarioService.crear(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(usuarioService.actualizar(id, body));
    }

    @PostMapping("/{id}/bloqueo")
    public ResponseEntity<?> toggleBloqueo(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.toggleBloqueo(id));
    }
}
