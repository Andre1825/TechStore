package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Marca;
import com.techstore.tech_store_project.repository.MarcaRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarcaService {

    private final MarcaRepository marcaRepository;
    private final ProductoRepository productoRepository;

    public MarcaService(MarcaRepository marcaRepository,
                        ProductoRepository productoRepository) {
        this.marcaRepository = marcaRepository;
        this.productoRepository = productoRepository;
    }

    public List<Map<String, Object>> listar() {
        return marcaRepository.findAll().stream().map(this::toDto).toList();
    }

    public Map<String, Object> crear(Marca marca) {
        if (marca.getNombre() == null || marca.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (marcaRepository.existsByNombreIgnoreCase(marca.getNombre())) {
            throw new ConflictoException("Ya existe una marca con ese nombre.");
        }
        marca.setActiva(true);
        marcaRepository.save(marca);
        return toDto(marca);
    }

    public Map<String, Object> actualizar(Long id, Marca cambios) {
        Marca existente = marcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada: " + id));

        if (!existente.getNombre().equalsIgnoreCase(cambios.getNombre())
                && marcaRepository.existsByNombreIgnoreCase(cambios.getNombre())) {
            throw new ConflictoException("Ya existe una marca con ese nombre.");
        }
        existente.setNombre(cambios.getNombre());
        existente.setDescripcion(cambios.getDescripcion());
        marcaRepository.save(existente);
        return toDto(existente);
    }

    public Map<String, Object> cambiarEstado(Long id) {
        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada: " + id));
        marca.setActiva(!marca.isActiva());
        marcaRepository.save(marca);
        return toDto(marca);
    }

    public void eliminar(Long id) {
        if (productoRepository.countByMarcaId(id) > 0) {
            throw new ConflictoException("No se puede eliminar: la marca tiene productos asociados.");
        }
        marcaRepository.deleteById(id);
    }

    private Map<String, Object> toDto(Marca m) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", m.getId());
        dto.put("nombre", m.getNombre());
        dto.put("descripcion", m.getDescripcion());
        dto.put("activa", m.isActiva());
        dto.put("productos", productoRepository.countByMarcaId(m.getId()));
        return dto;
    }
}
