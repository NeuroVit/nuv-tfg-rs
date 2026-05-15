package com.nuv.tfgrs.features.proveedores.controller;

import com.nuv.tfgrs.features.proveedores.dto.request.CrearProveedorRequest;
import com.nuv.tfgrs.features.proveedores.dto.request.EditarProveedorRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.proveedores.dto.response.ProveedorDetalleResponse;
import com.nuv.tfgrs.features.proveedores.dto.response.ProveedorResponse;
import com.nuv.tfgrs.features.proveedores.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProveedorResponse>> listar(
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived
    ) {
        return ResponseEntity.ok(proveedorService.listar(includeArchived));
    }

    @GetMapping("/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<ProveedorResponse>> listarPaginado(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(proveedorService.listarPaginado(status, search, limit, offset));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProveedorDetalleResponse> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.detalle(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody CrearProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProveedorResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EditarProveedorRequest request
    ) {
        return ResponseEntity.ok(proveedorService.actualizar(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProveedorResponse> archivar(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.archivar(id));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProveedorResponse> restaurar(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.restaurar(id));
    }
}
