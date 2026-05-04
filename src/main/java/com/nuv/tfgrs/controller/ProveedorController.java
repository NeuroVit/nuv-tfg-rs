package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.CrearProveedorRequest;
import com.nuv.tfgrs.dto.request.EditarProveedorRequest;
import com.nuv.tfgrs.dto.response.ProveedorResponse;
import com.nuv.tfgrs.service.ProveedorService;
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
