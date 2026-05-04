package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.CrearProveedorRequest;
import com.nuv.tfgrs.dto.request.EditarProveedorRequest;
import com.nuv.tfgrs.dto.response.ProveedorResponse;
import com.nuv.tfgrs.entity.Proveedor;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.ProductoRepository;
import com.nuv.tfgrs.repository.ProveedorRepository;
import com.nuv.tfgrs.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar(boolean includeArchived) {
        List<Proveedor> proveedores = includeArchived
                ? proveedorRepository.findAllByOrderByNameAsc()
                : proveedorRepository.findByActiveTrueOrderByNameAsc();

        return proveedores.stream()
                .map(proveedor -> ProveedorResponse.from(
                        proveedor,
                        productoRepository.countByProviderId(proveedor.getId())
                ))
                .toList();
    }

    @Override
    @Transactional
    public ProveedorResponse crear(CrearProveedorRequest request) {
        String nombre = normalizeRequired(request.nombre(), "El nombre del proveedor es obligatorio");
        if (proveedorRepository.existsByNameIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proveedor con ese nombre");
        }

        Proveedor proveedor = Proveedor.builder()
                .name(nombre)
                .warehouseLocation(normalizeRequired(request.ubicacionAlmacen(), "La ubicación del almacén es obligatoria"))
                .leadTimeDays(request.diasEntrega())
                .active(true)
                .build();

        Proveedor saved = proveedorRepository.save(proveedor);
        return ProveedorResponse.from(saved, 0);
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, EditarProveedorRequest request) {
        Proveedor proveedor = findByIdOrThrow(id);
        String nombre = normalizeRequired(request.nombre(), "El nombre del proveedor es obligatorio");
        if (proveedorRepository.existsByNameIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proveedor con ese nombre");
        }

        proveedor.setName(nombre);
        proveedor.setWarehouseLocation(normalizeRequired(
                request.ubicacionAlmacen(),
                "La ubicación del almacén es obligatoria"
        ));
        proveedor.setLeadTimeDays(request.diasEntrega());

        Proveedor saved = proveedorRepository.save(proveedor);
        return ProveedorResponse.from(saved, productoRepository.countByProviderId(saved.getId()));
    }

    @Override
    @Transactional
    public ProveedorResponse archivar(Long id) {
        Proveedor proveedor = findByIdOrThrow(id);
        proveedor.setActive(false);
        proveedor.setDeletedAt(LocalDateTime.now());
        Proveedor saved = proveedorRepository.save(proveedor);
        return ProveedorResponse.from(saved, productoRepository.countByProviderId(saved.getId()));
    }

    @Override
    @Transactional
    public ProveedorResponse restaurar(Long id) {
        Proveedor proveedor = findByIdOrThrow(id);
        proveedor.setActive(true);
        proveedor.setDeletedAt(null);
        Proveedor saved = proveedorRepository.save(proveedor);
        return ProveedorResponse.from(saved, productoRepository.countByProviderId(saved.getId()));
    }

    private Proveedor findByIdOrThrow(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
