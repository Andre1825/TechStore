package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Movimiento;
import com.techstore.tech_store_project.model.Producto;
import com.techstore.tech_store_project.repository.MovimientoRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import com.techstore.tech_store_project.dto.MovimientoRequest;
import com.techstore.tech_store_project.notification.StockBajoEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class MovimientoService {

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final ApplicationEventPublisher events;

    public MovimientoService(ProductoRepository productoRepository,
                             MovimientoRepository movimientoRepository, ApplicationEventPublisher events) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.events = events;
    }

    public List<Map<String, Object>> listarEntradas() {
        return movimientoRepository.findByTipoOrderByFechaDesc("ENTRADA").stream()
                .map(this::toDto).toList();
    }

    // RF-09: Registrar entrada sumando cantidad al stock actual
    @Transactional
    public Map<String, Object> guardarEntrada(@Valid MovimientoRequest body, String username) {
        return registrarMovimiento(body, username, "ENTRADA");
    }

    public List<Map<String, Object>> listarSalidas() {
        return movimientoRepository.findByTipoOrderByFechaDesc("SALIDA").stream()
                .map(this::toDto).toList();
    }

    // RF-10 + RF-11: Registrar salida validando que no supere el stock disponible
    @Transactional
    public Map<String, Object> guardarSalida(@Valid MovimientoRequest body, String username) {
        return registrarMovimiento(body, username, "SALIDA");
    }

    // RF-13: Consultar historial del Kardex filtrando por tipo y rango de fechas
    public List<Map<String, Object>> listarMovimientos(String tipo, String fechaDesde, String fechaHasta) {
        if (!tipo.isEmpty() && !List.of("ENTRADA", "SALIDA").contains(tipo)) {
            throw new IllegalArgumentException("El tipo debe ser ENTRADA o SALIDA.");
        }
        try {
            LocalDateTime desde = fechaDesde.isEmpty()
                    ? LocalDateTime.of(2000, 1, 1, 0, 0)
                    : LocalDate.parse(fechaDesde).atStartOfDay();
            LocalDateTime hasta = fechaHasta.isEmpty()
                    ? LocalDateTime.now().plusYears(100)
                    : LocalDate.parse(fechaHasta).atTime(LocalTime.MAX);

            if (desde.isAfter(hasta)) {
                throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final.");
            }

            return movimientoRepository.filtrar(tipo, desde, hasta).stream()
                    .map(this::toDto).toList();
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Las fechas deben ser válidas y usar el formato AAAA-MM-DD.");
        }
    }

    private Map<String, Object> registrarMovimiento(MovimientoRequest body, String username, String tipo) {
        Long productoId = body.productoId();
        int cantidad = body.cantidad();

        // El bloqueo se mantiene hasta confirmar stock y Kardex en la misma transacción.
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));
        if (!producto.isActivo()) {
            throw new ConflictoException("No se pueden registrar movimientos de un producto inactivo.");
        }

        int stockAnterior = producto.getStock();

        if ("SALIDA".equals(tipo)) {
            // RF-11: Validar que la cantidad no supere el stock disponible
            if (stockAnterior < cantidad) {
                throw new ConflictoException(
                        "Stock insuficiente: disponible " + stockAnterior + " unidades.");
            }
            producto.setStock(stockAnterior - cantidad);
        } else {
            if (cantidad > Integer.MAX_VALUE - stockAnterior) {
                throw new IllegalArgumentException("La entrada supera el stock máximo permitido.");
            }
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
        mov.setDocumentoRef(body.documentoRef() == null ? "" : body.documentoRef().trim());
        mov.setObservacion(body.observacion() == null ? "" : body.observacion().trim());
        movimientoRepository.save(mov);

        if ("SALIDA".equals(tipo) && producto.isStockBajo() && stockAnterior > producto.getStockMinimo()) {
            events.publishEvent(new StockBajoEvent(productoId, producto.getSku(), producto.getNombre(),
                    stockAnterior, producto.getStock(), producto.getStockMinimo(), mov.getId()));
        }

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
