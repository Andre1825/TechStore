package com.techstore.tech_store_project.repository;

import com.techstore.tech_store_project.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByTipoOrderByFechaDesc(String tipo);

    List<Movimiento> findAllByOrderByFechaDesc();

    List<Movimiento> findTop5ByOrderByFechaDesc();

    long countByTipo(String tipo);

    long countByProductoId(Long productoId);

    @Query("""
        SELECT m FROM Movimiento m
        WHERE (:tipo = '' OR m.tipo = :tipo)
          AND m.fecha >= :desde
          AND m.fecha <= :hasta
        ORDER BY m.fecha DESC
        """)
    List<Movimiento> filtrar(
            @Param("tipo") String tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT EXTRACT(YEAR FROM m.fecha), EXTRACT(MONTH FROM m.fecha), m.tipo, COUNT(m)
        FROM Movimiento m
        WHERE m.fecha >= :desde
        GROUP BY EXTRACT(YEAR FROM m.fecha), EXTRACT(MONTH FROM m.fecha), m.tipo
        ORDER BY EXTRACT(YEAR FROM m.fecha) ASC, EXTRACT(MONTH FROM m.fecha) ASC
        """)
    List<Object[]> contarMovimientosPorMes(@Param("desde") LocalDateTime desde);
}
