package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.response.AlertaStockResponse;
import com.nuv.tfgrs.service.AlertaStockService;
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

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<AlertaStockResponse> resolver(@PathVariable("id") Long id) {
        return ResponseEntity.ok(alertaStockService.resolver(id));
    }
}
