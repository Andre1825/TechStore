package com.techstore.tech_store_project.controller.api;

import com.techstore.tech_store_project.service.MovimientoService;
import com.techstore.tech_store_project.dto.MovimientoRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RF-09..RF-13: API REST del Kardex (entradas, salidas y movimientos) para React.
 */
@RestController
@RequestMapping("/api")
public class MovimientoApiController {

    private final MovimientoService movimientoService;

    public MovimientoApiController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @GetMapping("/entradas")
    public List<Map<String, Object>> listarEntradas() {
        return movimientoService.listarEntradas();
    }

    @PostMapping("/entradas")
    public ResponseEntity<?> guardarEntrada(@Valid @RequestBody MovimientoRequest body, Authentication auth) {
        return ResponseEntity.ok(movimientoService.guardarEntrada(body, auth != null ? auth.getName() : null));
    }

    @GetMapping("/salidas")
    public List<Map<String, Object>> listarSalidas() {
        return movimientoService.listarSalidas();
    }

    @PostMapping("/salidas")
    public ResponseEntity<?> guardarSalida(@Valid @RequestBody MovimientoRequest body, Authentication auth) {
        return ResponseEntity.ok(movimientoService.guardarSalida(body, auth != null ? auth.getName() : null));
    }

    @GetMapping("/movimientos")
    public List<Map<String, Object>> listarMovimientos(
            @RequestParam(required = false, defaultValue = "") String tipo,
            @RequestParam(required = false, defaultValue = "") String fechaDesde,
            @RequestParam(required = false, defaultValue = "") String fechaHasta) {
        return movimientoService.listarMovimientos(tipo, fechaDesde, fechaHasta);
    }
}
