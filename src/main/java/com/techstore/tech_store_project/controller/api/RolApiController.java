package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.service.RolService;
import com.techstore.tech_store_project.dto.RolRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RF-03: API REST de roles y permisos (RBAC) para el frontend React.
 */
@RestController
@RequestMapping("/api/roles")
public class RolApiController {

    private final RolService rolService;

    public RolApiController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping("/permisos")
    public List<Map<String, String>> permisos() {
        return rolService.permisos();
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return rolService.listar();
    }

    @GetMapping("/activos")
    public List<Map<String, Object>> activos() {
        return rolService.activos();
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody RolRequest body) {
        return ResponseEntity.ok(rolService.crear(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody RolRequest body) {
        return ResponseEntity.ok(rolService.actualizar(id, body));
    }
}
