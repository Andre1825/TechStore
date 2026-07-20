package com.techstore.tech_store_project.service;

import com.techstore.tech_store_project.model.Categoria;
import com.techstore.tech_store_project.repository.CategoriaRepository;
import com.techstore.tech_store_project.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    public List<Map<String, Object>> listar() {
        return categoriaRepository.findAll().stream().map(this::toDto).toList();
    }

    // RF-04: Registrar categoría validando nombre único y generando código automático
    public Map<String, Object> crear(Categoria categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (categoriaRepository.existsByNombreIgnoreCase(categoria.getNombre())) {
            throw new ConflictoException("Ya existe una categoría con ese nombre.");
        }
        long total = categoriaRepository.count() + 1;
        categoria.setCodigo(String.format("CAT-%02d", total));
        categoria.setActiva(true);
        categoriaRepository.save(categoria);
        return toDto(categoria);
    }

    public Map<String, Object> actualizar(Long id, Categoria cambios) {
        Categoria existente = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));

        if (!existente.getNombre().equalsIgnoreCase(cambios.getNombre())
                && categoriaRepository.existsByNombreIgnoreCase(cambios.getNombre())) {
            throw new ConflictoException("Ya existe una categoría con ese nombre.");
        }
        existente.setNombre(cambios.getNombre());
        existente.setDescripcion(cambios.getDescripcion());
        categoriaRepository.save(existente);
        return toDto(existente);
    }

    public Map<String, Object> cambiarEstado(Long id) {
        Categoria cat = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada: " + id));
        cat.setActiva(!cat.isActiva());
        categoriaRepository.save(cat);
        return toDto(cat);
    }

    public void eliminar(Long id) {
        if (productoRepository.countByCategoriaId(id) > 0) {
            throw new ConflictoException("No se puede eliminar: la categoría tiene productos asociados.");
        }
        categoriaRepository.deleteById(id);
    }

    private Map<String, Object> toDto(Categoria c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("codigo", c.getCodigo());
        m.put("nombre", c.getNombre());
        m.put("descripcion", c.getDescripcion());
        m.put("activa", c.isActiva());
        m.put("productos", productoRepository.countByCategoriaId(c.getId()));
        return m;
    }
}
