package com.nuv.tfgrs.features.alertas.controller;

import com.nuv.tfgrs.features.alertas.dto.response.AlertaResumenResponse;
import com.nuv.tfgrs.features.alertas.dto.response.AlertaStockResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.alertas.service.AlertaStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
public class AlertaStockController {

    private final AlertaStockService alertaStockService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AlertaStockResponse>> listar(
            @RequestParam(name = "resueltas", required = false) Boolean resueltas
    ) {
        return ResponseEntity.ok(alertaStockService.listar(resueltas));
    }

    @GetMapping("/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<AlertaStockResponse>> listarPaginado(
            @RequestParam(name = "resueltas", required = false) Boolean resueltas,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(alertaStockService.listarPaginado(resueltas, search, limit, offset));
    }

    @GetMapping("/resumen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlertaResumenResponse> resumen() {
        return ResponseEntity.ok(alertaStockService.resumen());
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlertaStockResponse> resolver(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertaStockService.resolver(id));
    }
}
