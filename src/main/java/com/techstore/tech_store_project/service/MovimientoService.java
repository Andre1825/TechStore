package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Movimiento;
import com.techstore.tech_store_project.model.Producto;
import com.techstore.tech_store_project.repository.MovimientoRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovimientoService {

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;

    public MovimientoService(ProductoRepository productoRepository,
                             MovimientoRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public List<Map<String, Object>> listarEntradas() {
        return movimientoRepository.findByTipoOrderByFechaDesc("ENTRADA").stream()
                .map(this::toDto).toList();
    }

    // RF-09: Registrar entrada sumando cantidad al stock actual
    public Map<String, Object> guardarEntrada(Map<String, Object> body, String username) {
        return registrarMovimiento(body, username, "ENTRADA");
    }

    public List<Map<String, Object>> listarSalidas() {
        return movimientoRepository.findByTipoOrderByFechaDesc("SALIDA").stream()
                .map(this::toDto).toList();
    }

    // RF-10 + RF-11: Registrar salida validando que no supere el stock disponible
    public Map<String, Object> guardarSalida(Map<String, Object> body, String username) {
        return registrarMovimiento(body, username, "SALIDA");
    }

    // RF-13: Consultar historial del Kardex filtrando por tipo y rango de fechas
    public List<Map<String, Object>> listarMovimientos(String tipo, String fechaDesde, String fechaHasta) {
        LocalDateTime desde = fechaDesde.isEmpty()
                ? LocalDateTime.of(2000, 1, 1, 0, 0)
                : LocalDate.parse(fechaDesde).atStartOfDay();
        LocalDateTime hasta = fechaHasta.isEmpty()
                ? LocalDateTime.now().plusYears(100)
                : LocalDate.parse(fechaHasta).atTime(LocalTime.MAX);

        return movimientoRepository.filtrar(tipo, desde, hasta).stream()
                .map(this::toDto).toList();
    }

    private Map<String, Object> registrarMovimiento(Map<String, Object> body, String username, String tipo) {
        Long productoId = body.get("productoId") == null ? null
                : Long.valueOf(String.valueOf(body.get("productoId")));
        Integer cantidad = body.get("cantidad") == null ? null
                : (int) Double.parseDouble(String.valueOf(body.get("cantidad")));

        if (productoId == null || cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("Producto y cantidad (> 0) son obligatorios.");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));

        int stockAnterior = producto.getStock();

        if ("SALIDA".equals(tipo)) {
            // RF-11: Validar que la cantidad no supere el stock disponible
            if (stockAnterior < cantidad) {
                throw new ConflictoException(
                        "Stock insuficiente: disponible " + stockAnterior + " unidades.");
            }
            producto.setStock(stockAnterior - cantidad);
        } else {
            producto.setStock(stockAnterior + cantidad);
        }
        productoRepository.save(producto);

        // RF-12: Registro inalterable en el Kardex para auditoría
        Movimiento mov = new Movimiento();
        mov.setProducto(producto);
        mov.setTipo(tipo);
        mov.setCantidad(cantidad);
        mov.setStockAnterior(stockAnterior);
        mov.setStockResultante(producto.getStock());
        mov.setFecha(LocalDateTime.now());
        mov.setUsuarioNombre(username != null ? username : "sistema");
        mov.setDocumentoRef(String.valueOf(body.getOrDefault("documentoRef", "")));
        mov.setObservacion(String.valueOf(body.getOrDefault("observacion", "")));
        movimientoRepository.save(mov);

        return toDto(mov);
    }

    private Map<String, Object> toDto(Movimiento mov) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mov.getId());
        m.put("productoId", mov.getProducto().getId());
        m.put("producto", mov.getProducto().getNombre());
        m.put("sku", mov.getProducto().getSku());
        m.put("tipo", mov.getTipo());
        m.put("cantidad", mov.getCantidad());
        m.put("stockAnterior", mov.getStockAnterior());
        m.put("stockResultante", mov.getStockResultante());
        m.put("fecha", mov.getFecha());
        m.put("usuario", mov.getUsuarioNombre());
        m.put("documentoRef", mov.getDocumentoRef());
        m.put("observacion", mov.getObservacion());
        return m;
    }
}
