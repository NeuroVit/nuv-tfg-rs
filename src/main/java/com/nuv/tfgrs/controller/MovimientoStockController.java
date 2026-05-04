package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.response.MovimientoStockPageResponse;
import com.nuv.tfgrs.service.MovimientoStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockQueryService movimientoStockQueryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MovimientoStockPageResponse> listar(
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "razon", required = false) String razon,
            @RequestParam(name = "desde", required = false) LocalDate desde,
            @RequestParam(name = "hasta", required = false) LocalDate hasta,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(
                movimientoStockQueryService.listar(productId, tipo, razon, desde, hasta, limit, offset)
        );
    }
}
