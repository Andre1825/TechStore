package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RF-05..RF-08: API REST de productos para el frontend React.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoApiController {

    private final ProductoService productoService;

    public ProductoApiController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public Map<String, Object> listar(
            @RequestParam(required = false, defaultValue = "") String sku,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false, defaultValue = "0") int page) {
        return productoService.listar(sku, categoriaId, marcaId, activo, page);
    }

    @GetMapping("/activos")
    public List<Map<String, Object>> activos() {
        return productoService.activos();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(productoService.crear(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body,
                                        Authentication auth) {
        String username = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(productoService.actualizar(id, body, username));
    }

    @PostMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.cambiarEstado(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/validar-sku")
    public Map<String, Boolean> validarSku(@RequestParam String sku) {
        return Map.of("existe", productoService.existeSku(sku));
    }

    @GetMapping("/{id}/precios")
    public List<Map<String, Object>> historialPrecios(@PathVariable Long id) {
        return productoService.historialPrecios(id);
    }
}
