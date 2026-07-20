package com.techstore.tech_store_project.repository;

import com.techstore.tech_store_project.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    Optional<Rol> findByNombreIgnoreCase(String nombre);
    List<Rol> findByActivoTrue();
}
