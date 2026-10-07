package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.service.UsuarioService;
import com.techstore.tech_store_project.dto.UsuarioCreateRequest;
import com.techstore.tech_store_project.dto.UsuarioUpdateRequest;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> crear(@Valid @RequestBody UsuarioCreateRequest body) {
        return ResponseEntity.ok(usuarioService.crear(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest body) {
        return ResponseEntity.ok(usuarioService.actualizar(id, body));
    }

    @PostMapping("/{id}/bloqueo")
    public ResponseEntity<?> toggleBloqueo(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.toggleBloqueo(id));
    }
}
