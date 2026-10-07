package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.model.Categoria;
import com.techstore.tech_store_project.service.CategoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/**
 * RF-04: API REST de categorías para el frontend React.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaApiController {

    private final CategoriaService categoriaService;

    public CategoriaApiController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return categoriaService.listar();
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody Categoria categoria) {
        return ResponseEntity.ok(categoriaService.crear(categoria));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody Categoria cambios) {
        return ResponseEntity.ok(categoriaService.actualizar(id, cambios));
    }

    @PostMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.cambiarEstado(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
