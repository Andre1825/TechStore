package com.techstore.tech_store_project.repository;

import com.techstore.tech_store_project.model.PrecioHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrecioHistorialRepository extends JpaRepository<PrecioHistorial, Long> {
    List<PrecioHistorial> findByProductoIdOrderByFechaDesc(Long productoId);
}
