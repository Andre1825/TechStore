package com.techstore.tech_store_project.config;

import com.techstore.tech_store_project.service.ConflictoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.Map;

/** Manejo global de errores del API: responde siempre JSON con {"mensaje": ...}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(com.techstore.tech_store_project.service.ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> handleUnavailable(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).distinct().sorted()
                .collect(java.util.stream.Collectors.joining(" "));
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraint(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream().map(v -> v.getMessage()).distinct().sorted()
                .collect(java.util.stream.Collectors.joining(" "));
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje",
                "Datos inválidos: verifica los tipos, las cantidades enteras y los campos obligatorios."));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> handleDatabaseConflict(Exception ex) {
        log.warn("Conflicto de integridad o concurrencia", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",
                "La operación entra en conflicto con los datos actuales. Actualiza la información e inténtalo de nuevo."));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, String>> handleConflicto(ConflictoException ex) {
        log.warn("ConflictoException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("mensaje", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensaje", "Ocurrió un error inesperado. Contacta al administrador."));
    }
}
