package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Categoria;
import com.techstore.tech_store_project.model.Marca;
import com.techstore.tech_store_project.model.PrecioHistorial;
import com.techstore.tech_store_project.model.Producto;
import com.techstore.tech_store_project.repository.MovimientoRepository;
import com.techstore.tech_store_project.repository.PrecioHistorialRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductoService {

    private static final int PAGE_SIZE = 12;

    private final ProductoRepository productoRepository;
    private final PrecioHistorialRepository precioHistorialRepository;
    private final MovimientoRepository movimientoRepository;

    public ProductoService(ProductoRepository productoRepository,
                           PrecioHistorialRepository precioHistorialRepository,
                           MovimientoRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.precioHistorialRepository = precioHistorialRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // RF-08: Filtrar productos por SKU/nombre, categoría, marca y estado (paginado)
    public Map<String, Object> listar(String sku, Long categoriaId, Long marcaId, Boolean activo, int page) {
        if (page < 0) page = 0;
        Page<Producto> pagina = productoRepository.filtrar(
                sku.trim(), categoriaId, marcaId, activo, PageRequest.of(page, PAGE_SIZE));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("contenido", pagina.getContent().stream().map(this::toDto).toList());
        resp.put("paginaActual", page);
        resp.put("totalPaginas", pagina.getTotalPages());
        resp.put("totalElementos", pagina.getTotalElements());
        return resp;
    }

    public List<Map<String, Object>> activos() {
        return productoRepository.findAll().stream()
                .filter(Producto::isActivo)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("sku", p.getSku());
                    m.put("nombre", p.getNombre());
                    m.put("stock", p.getStock());
                    return m;
                }).toList();
    }

    // RF-05: Registrar producto validando SKU único
    public Map<String, Object> crear(Map<String, Object> body) {
        String sku = str(body.get("sku"));
        if (sku.isBlank() || str(body.get("nombre")).isBlank() || body.get("categoriaId") == null) {
            throw new IllegalArgumentException("SKU, nombre y categoría son obligatorios.");
        }
        if (productoRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictoException("Ya existe un producto con ese SKU.");
        }

        Producto p = new Producto();
        p.setSku(sku.trim());
        p.setNombre(str(body.get("nombre")).trim());
        p.setDescripcion(str(body.get("descripcion")));
        p.setPrecio(dbl(body.get("precio")));
        p.setStockMinimo(intg(body.get("stockMinimo")));
        p.setStock(0);
        p.setActivo(true);
        p.setCategoria(refCategoria(body.get("categoriaId")));
        p.setMarca(refMarca(body.get("marcaId")));

        productoRepository.save(p);
        return toDto(p);
    }

    // RF-06: Actualizar producto manteniendo el SKU inalterable + historial de precios
    public Map<String, Object> actualizar(Long id, Map<String, Object> body, String username) {
        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));

        Double nuevoPrecio = dbl(body.get("precio"));
        // RF-06: Registrar cambios de precio en historial para auditoría
        if (!existente.getPrecio().equals(nuevoPrecio)) {
            PrecioHistorial hist = new PrecioHistorial();
            hist.setProducto(existente);
            hist.setPrecioAnterior(existente.getPrecio());
            hist.setPrecioNuevo(nuevoPrecio);
            hist.setFecha(LocalDateTime.now());
            hist.setUsuarioNombre(username != null ? username : "sistema");
            precioHistorialRepository.save(hist);
        }

        existente.setNombre(str(body.get("nombre")).trim());
        existente.setDescripcion(str(body.get("descripcion")));
        existente.setPrecio(nuevoPrecio);
        existente.setStockMinimo(intg(body.get("stockMinimo")));
        existente.setCategoria(refCategoria(body.get("categoriaId")));
        existente.setMarca(refMarca(body.get("marcaId")));

        productoRepository.save(existente);
        return toDto(existente);
    }

    // RF-07: Baja lógica — invertir estado Activo/Inactivo sin borrar el registro
    public Map<String, Object> cambiarEstado(Long id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        p.setActivo(!p.isActivo());
        productoRepository.save(p);
        return toDto(p);
    }

    public void eliminar(Long id) {
        if (movimientoRepository.countByProductoId(id) > 0) {
            throw new ConflictoException(
                    "No se puede eliminar: el producto tiene movimientos registrados. Use la baja lógica.");
        }
        productoRepository.deleteById(id);
    }

    // RF-05: Validación de SKU único (en vivo desde el formulario)
    public boolean existeSku(String sku) {
        return productoRepository.existsBySkuIgnoreCase(sku.trim());
    }

    // RF-06: Historial de precios de un producto
    public List<Map<String, Object>> historialPrecios(Long id) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return precioHistorialRepository.findByProductoIdOrderByFechaDesc(id).stream()
                .map(h -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fecha", h.getFecha().format(fmt));
                    m.put("anterior", h.getPrecioAnterior());
                    m.put("nuevo", h.getPrecioNuevo());
                    m.put("usuario", h.getUsuarioNombre());
                    return m;
                }).toList();
    }

    private Map<String, Object> toDto(Producto p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("sku", p.getSku());
        m.put("nombre", p.getNombre());
        m.put("descripcion", p.getDescripcion());
        m.put("precio", p.getPrecio());
        m.put("stock", p.getStock());
        m.put("stockMinimo", p.getStockMinimo());
        m.put("activo", p.isActivo());
        m.put("stockBajo", p.isStockBajo());
        m.put("categoriaId", p.getCategoria() != null ? p.getCategoria().getId() : null);
        m.put("categoria", p.getCategoria() != null ? p.getCategoria().getNombre() : null);
        m.put("marcaId", p.getMarca() != null ? p.getMarca().getId() : null);
        m.put("marca", p.getMarca() != null ? p.getMarca().getNombre() : null);
        return m;
    }

    private Categoria refCategoria(Object id) {
        if (id == null) return null;
        Categoria c = new Categoria();
        c.setId(lng(id));
        return c;
    }

    private Marca refMarca(Object id) {
        if (id == null || str(id).isBlank()) return null;
        Marca mr = new Marca();
        mr.setId(lng(id));
        return mr;
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private Long lng(Object o) { return o == null ? null : Long.valueOf(String.valueOf(o)); }
    private Double dbl(Object o) { return o == null || str(o).isBlank() ? 0.0 : Double.valueOf(String.valueOf(o)); }
    private Integer intg(Object o) { return o == null || str(o).isBlank() ? 0 : (int) Double.parseDouble(String.valueOf(o)); }
}
