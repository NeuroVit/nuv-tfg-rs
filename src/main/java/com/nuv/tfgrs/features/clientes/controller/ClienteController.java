package com.nuv.tfgrs.features.clientes.controller;

import com.nuv.tfgrs.features.clientes.dto.request.CrearClienteRequest;
import com.nuv.tfgrs.features.clientes.dto.request.EditarClienteRequest;
import com.nuv.tfgrs.features.clientes.dto.response.ClienteResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.clientes.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClienteResponse>> listar(
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived
    ) {
        return ResponseEntity.ok(clienteService.listar(includeArchived));
    }

    @GetMapping("/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<ClienteResponse>> listarPaginado(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(clienteService.listarPaginado(status, search, limit, offset));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody CrearClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ClienteResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EditarClienteRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ClienteResponse> archivar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.archivar(id));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ClienteResponse> restaurar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.restaurar(id));
    }
}
