package com.techstore.tech_store_project.repository;

import com.techstore.tech_store_project.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.username = :username")
    Optional<Usuario> findByUsernameForUpdate(@Param("username") String username);

    boolean existsByUsername(String username);

    boolean existsByCorreo(String correo);
    List<Usuario> findByRolId(Long rolId);
    List<Usuario> findByActivoTrueAndCuentaBloqueadaFalseAndStockAlertasActivasTrue();
}
