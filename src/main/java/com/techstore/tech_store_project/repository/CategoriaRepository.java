package com.techstore.tech_store_project.repository;

import com.techstore.tech_store_project.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByCodigoIgnoreCase(String codigo);
}