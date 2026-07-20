package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.model.Marca;
import com.techstore.tech_store_project.service.MarcaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST de marcas para el frontend React.
 */
@RestController
@RequestMapping("/api/marcas")
public class MarcaApiController {

    private final MarcaService marcaService;

    public MarcaApiController(MarcaService marcaService) {
        this.marcaService = marcaService;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return marcaService.listar();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Marca marca) {
        return ResponseEntity.ok(marcaService.crear(marca));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Marca cambios) {
        return ResponseEntity.ok(marcaService.actualizar(id, cambios));
    }

    @PostMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(marcaService.cambiarEstado(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        marcaService.eliminar(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
