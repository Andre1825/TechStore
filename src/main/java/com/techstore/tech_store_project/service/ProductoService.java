package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Categoria;
import com.techstore.tech_store_project.model.Marca;
import com.techstore.tech_store_project.model.PrecioHistorial;
import com.techstore.tech_store_project.model.Producto;
import com.techstore.tech_store_project.repository.MovimientoRepository;
import com.techstore.tech_store_project.repository.PrecioHistorialRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import com.techstore.tech_store_project.repository.CategoriaRepository;
import com.techstore.tech_store_project.repository.MarcaRepository;
import com.techstore.tech_store_project.dto.ProductoRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Validated
public class ProductoService {

    private static final int PAGE_SIZE = 12;

    private final ProductoRepository productoRepository;
    private final PrecioHistorialRepository precioHistorialRepository;
    private final MovimientoRepository movimientoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           PrecioHistorialRepository precioHistorialRepository,
                           MovimientoRepository movimientoRepository,
                           CategoriaRepository categoriaRepository,
                           MarcaRepository marcaRepository) {
        this.productoRepository = productoRepository;
        this.precioHistorialRepository = precioHistorialRepository;
        this.movimientoRepository = movimientoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
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
    @Transactional
    public Map<String, Object> crear(@Valid ProductoRequest body) {
        String sku = body.sku() == null ? "" : body.sku().trim();
        if (sku.isBlank()) {
            throw new IllegalArgumentException("El SKU es obligatorio.");
        }
        if (productoRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictoException("Ya existe un producto con ese SKU.");
        }

        Producto p = new Producto();
        p.setSku(sku.trim());
        p.setNombre(body.nombre().trim());
        p.setDescripcion(body.descripcion());
        p.setPrecio(body.precio().setScale(2, RoundingMode.UNNECESSARY));
        p.setStockMinimo(body.stockMinimo());
        p.setStock(0);
        p.setActivo(true);
        p.setCategoria(refCategoria(body.categoriaId()));
        p.setMarca(refMarca(body.marcaId()));

        productoRepository.save(p);
        return toDto(p);
    }

    // RF-06: Actualizar producto manteniendo el SKU inalterable + historial de precios
    @Transactional
    public Map<String, Object> actualizar(Long id, @Valid ProductoRequest body, String username) {
        Producto existente = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));

        BigDecimal nuevoPrecio = body.precio().setScale(2, RoundingMode.UNNECESSARY);
        // RF-06: Registrar cambios de precio en historial para auditoría
        if (existente.getPrecio().compareTo(nuevoPrecio) != 0) {
            PrecioHistorial hist = new PrecioHistorial();
            hist.setProducto(existente);
            hist.setPrecioAnterior(existente.getPrecio());
            hist.setPrecioNuevo(nuevoPrecio);
            hist.setFecha(LocalDateTime.now());
            hist.setUsuarioNombre(username != null ? username : "sistema");
            precioHistorialRepository.save(hist);
        }

        existente.setNombre(body.nombre().trim());
        existente.setDescripcion(body.descripcion());
        existente.setPrecio(nuevoPrecio);
        existente.setStockMinimo(body.stockMinimo());
        existente.setCategoria(refCategoria(body.categoriaId()));
        existente.setMarca(refMarca(body.marcaId()));

        productoRepository.save(existente);
        return toDto(existente);
    }

    // RF-07: Baja lógica — invertir estado Activo/Inactivo sin borrar el registro
    @Transactional
    public Map<String, Object> cambiarEstado(Long id) {
        Producto p = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        p.setActivo(!p.isActivo());
        productoRepository.save(p);
        return toDto(p);
    }

    @Transactional
    public void eliminar(Long id) {
        Producto p = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
        if (movimientoRepository.countByProductoId(id) > 0) {
            throw new ConflictoException(
                    "No se puede eliminar: el producto tiene movimientos registrados. Use la baja lógica.");
        }
        productoRepository.delete(p);
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

    private Categoria refCategoria(Long id) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe."));
        if (!c.isActiva()) throw new IllegalArgumentException("La categoría está inactiva.");
        return c;
    }

    private Marca refMarca(Long id) {
        if (id == null) return null;
        Marca mr = marcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La marca no existe."));
        if (!mr.isActiva()) throw new IllegalArgumentException("La marca está inactiva.");
        return mr;
    }

}
