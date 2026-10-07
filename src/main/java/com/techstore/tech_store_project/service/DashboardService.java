package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Movimiento;
import com.techstore.tech_store_project.model.Producto;
import com.techstore.tech_store_project.repository.CategoriaRepository;
import com.techstore.tech_store_project.repository.MarcaRepository;
import com.techstore.tech_store_project.repository.MovimientoRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import com.techstore.tech_store_project.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoRepository movimientoRepository;

    public DashboardService(ProductoRepository productoRepository,
                            CategoriaRepository categoriaRepository,
                            MarcaRepository marcaRepository,
                            UsuarioRepository usuarioRepository,
                            MovimientoRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // RF-15: Conteo de productos con stock bajo (badge del navbar, se refresca cada 30 s)
    public Map<String, Long> stockBajoCount() {
        return Map.of("count", productoRepository.countProductosConStockBajo());
    }

    // RF-15: Lista de productos con stock bajo (panel de notificaciones del navbar)
    public List<Map<String, Object>> stockBajo() {
        return productoRepository.findProductosConStockBajo().stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "nombre", p.getNombre(),
                        "sku", p.getSku(),
                        "stock", p.getStock(),
                        "stockMinimo", p.getStockMinimo()
                )).toList();
    }

    // RF-14 / RF-15: Datos completos del dashboard para el frontend React
    public Map<String, Object> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();

        long totalProductos = productoRepository.count();
        // RF-14: Total de productos contabilizando solo los activos
        long totalActivos = productoRepository.countByActivoTrue();

        data.put("totalProductos", totalProductos);
        data.put("totalActivos", totalActivos);
        data.put("totalInactivos", totalProductos - totalActivos);
        data.put("totalCategorias", categoriaRepository.count());
        data.put("totalMarcas", marcaRepository.count());
        data.put("totalUsuarios", usuarioRepository.count());
        data.put("totalEntradas", movimientoRepository.countByTipo("ENTRADA"));
        data.put("totalSalidas", movimientoRepository.countByTipo("SALIDA"));

        BigDecimal valorInventario = productoRepository.calcularValorInventario();
        data.put("valorInventario", valorInventario != null ? valorInventario : BigDecimal.ZERO);

        // RF-15: Alertas de stock bajo
        List<Producto> stockBajo = productoRepository.findProductosConStockBajo();
        data.put("stockBajoCount", stockBajo.size());
        data.put("productosStockBajo", stockBajo.stream().map(p -> Map.of(
                "id", p.getId(),
                "nombre", p.getNombre(),
                "sku", p.getSku(),
                "stock", p.getStock(),
                "stockMinimo", p.getStockMinimo()
        )).toList());

        // Productos activos por categoría (gráfico de barras)
        List<String> categoriaLabels = new ArrayList<>();
        List<Long> categoriaCounts = new ArrayList<>();
        for (Object[] row : productoRepository.countActivosByCategoria()) {
            categoriaLabels.add((String) row[0]);
            categoriaCounts.add((Long) row[1]);
        }
        data.put("categoriaLabels", categoriaLabels);
        data.put("categoriaCounts", categoriaCounts);

        // Movimientos por mes — últimos 6 meses (gráfico de líneas)
        LocalDateTime desde6meses = LocalDateTime.now().minusMonths(6);
        List<Object[]> movData = movimientoRepository.contarMovimientosPorMes(desde6meses);

        DateTimeFormatter fmtMes = DateTimeFormatter.ofPattern("MMM yy", Locale.forLanguageTag("es"));
        List<String> mesesLabels = new ArrayList<>();
        List<Long> entradasMes = new ArrayList<>();
        List<Long> salidasMes = new ArrayList<>();

        LocalDate hoy = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate mes = hoy.minusMonths(i);
            int anio = mes.getYear();
            int numMes = mes.getMonthValue();
            mesesLabels.add(mes.format(fmtMes));

            long ent = movData.stream()
                    .filter(r -> ((Number) r[0]).intValue() == anio
                            && ((Number) r[1]).intValue() == numMes
                            && "ENTRADA".equals(r[2]))
                    .mapToLong(r -> ((Number) r[3]).longValue()).sum();
            long sal = movData.stream()
                    .filter(r -> ((Number) r[0]).intValue() == anio
                            && ((Number) r[1]).intValue() == numMes
                            && "SALIDA".equals(r[2]))
                    .mapToLong(r -> ((Number) r[3]).longValue()).sum();

            entradasMes.add(ent);
            salidasMes.add(sal);
        }
        data.put("mesesLabels", mesesLabels);
        data.put("entradasMes", entradasMes);
        data.put("salidasMes", salidasMes);

        // Últimos 5 movimientos
        data.put("ultimosMovimientos", movimientoRepository.findTop5ByOrderByFechaDesc().stream()
                .map(this::movimientoResumen).toList());

        return data;
    }

    private Map<String, Object> movimientoResumen(Movimiento mov) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", mov.getId());
        m.put("producto", mov.getProducto().getNombre());
        m.put("tipo", mov.getTipo());
        m.put("cantidad", mov.getCantidad());
        m.put("fecha", mov.getFecha());
        return m;
    }
}
