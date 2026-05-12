package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.CrearCategoriaRequest;
import com.nuv.tfgrs.dto.request.EditarCategoriaRequest;
import com.nuv.tfgrs.dto.response.CategoriaResponse;
import com.nuv.tfgrs.entity.Categoria;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.CategoriaRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas() {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        return categoriaRepository.findAllByOrganizationIdOrderByNameAsc(organizationId)
                .stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        return CategoriaResponse.from(buscarEntidadPorId(id, organizationId));
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CrearCategoriaRequest request) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        String nombreNormalizado = normalizarNombre(request.nombre());
        validarNombreDisponibleParaCrear(nombreNormalizado, organizationId);

        Categoria categoria = Categoria.builder()
                .organizationId(organizationId)
                .name(nombreNormalizado)
                .description(normalizarDescripcion(request.descripcion()))
                .build();

        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Long id, EditarCategoriaRequest request) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Categoria categoria = buscarEntidadPorId(id, organizationId);

        if (request.nombre() != null) {
            String nombreNormalizado = normalizarNombre(request.nombre());
            validarNombreDisponibleParaActualizar(nombreNormalizado, organizationId, id);
            categoria.setName(nombreNormalizado);
        }

        if (request.descripcion() != null) {
            categoria.setDescription(normalizarDescripcion(request.descripcion()));
        }

        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Categoria categoria = buscarEntidadPorId(id, organizationId);
        try {
            categoriaRepository.delete(categoria);
            categoriaRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar la categoría porque tiene productos asociados"
            );
        }
    }

    private Categoria buscarEntidadPorId(Long id, Long organizationId) {
        return categoriaRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + id));
    }

    private void validarNombreDisponibleParaCrear(String nombre, Long organizationId) {
        if (categoriaRepository.existsByNameIgnoreCaseAndOrganizationId(nombre, organizationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya está en uso");
        }
    }

    private void validarNombreDisponibleParaActualizar(String nombre, Long organizationId, Long idCategoria) {
        if (categoriaRepository.existsByNameIgnoreCaseAndOrganizationIdAndIdNot(nombre, organizationId, idCategoria)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de la categoría ya está en uso");
        }
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no puede estar vacío");
        }
        return nombre.trim();
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String limpia = descripcion.trim();
        return limpia.isEmpty() ? null : limpia;
    }
}

