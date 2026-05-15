package com.nuv.tfgrs.features.ventas.controller;

import com.nuv.tfgrs.features.ventas.dto.request.CrearVentaRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.ventas.dto.response.VentaResponse;
import com.nuv.tfgrs.features.ventas.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<VentaResponse>> listar(
            @RequestParam(name = "clientId", required = false) Long clientId,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(ventaService.listar(clientId, limit));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<PagedResponse<VentaResponse>> listarPaginado(
            @RequestParam(name = "clientId", required = false) Long clientId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) LocalDate toDate,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(ventaService.listarPaginado(clientId, search, fromDate, toDate, limit, offset));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VentaResponse> crear(
            @Valid @RequestBody CrearVentaRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaService.crear(request, authentication.getName()));
    }
}
