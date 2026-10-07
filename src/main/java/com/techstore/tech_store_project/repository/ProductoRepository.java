package com.techstore.tech_store_project.repository;

import com.techstore.tech_store_project.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    boolean existsBySkuIgnoreCase(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    @Query(value = """
        SELECT p FROM Producto p LEFT JOIN p.marca m
        WHERE (:sku IS NULL OR :sku = '' OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :sku, '%')))
          AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
          AND (:marcaId IS NULL OR m.id = :marcaId)
          AND (:activo IS NULL OR p.activo = :activo)
        ORDER BY p.nombre ASC
        """,
        countQuery = """
        SELECT COUNT(p) FROM Producto p LEFT JOIN p.marca m
        WHERE (:sku IS NULL OR :sku = '' OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :sku, '%')))
          AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)
          AND (:marcaId IS NULL OR m.id = :marcaId)
          AND (:activo IS NULL OR p.activo = :activo)
        """)
    Page<Producto> filtrar(
            @Param("sku") String sku,
            @Param("categoriaId") Long categoriaId,
            @Param("marcaId") Long marcaId,
            @Param("activo") Boolean activo,
            Pageable pageable
    );

    long countByActivoTrue();

    long countByCategoriaId(Long categoriaId);

    long countByMarcaId(Long marcaId);

    @Query("SELECT p FROM Producto p WHERE p.activo = true AND p.stockMinimo > 0 AND p.stock <= p.stockMinimo ORDER BY p.stock ASC")
    List<Producto> findProductosConStockBajo();

    @Query("SELECT COUNT(p) FROM Producto p WHERE p.activo = true AND p.stockMinimo > 0 AND p.stock <= p.stockMinimo")
    long countProductosConStockBajo();

    @Query("SELECT p.categoria.nombre, COUNT(p) FROM Producto p WHERE p.activo = true GROUP BY p.categoria.nombre ORDER BY COUNT(p) DESC")
    List<Object[]> countActivosByCategoria();

    @Query("SELECT SUM(p.precio * p.stock) FROM Producto p WHERE p.activo = true")
    BigDecimal calcularValorInventario();
}
