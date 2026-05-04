package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.CrearVentaRequest;
import com.nuv.tfgrs.dto.response.VentaResponse;
import com.nuv.tfgrs.service.VentaService;
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

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VentaResponse>> listar(
            @RequestParam(name = "clientId", required = false) Long clientId,
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(ventaService.listar(clientId, limit));
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
