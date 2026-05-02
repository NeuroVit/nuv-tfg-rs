package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.response.MovimientoStockResponse;
import com.nuv.tfgrs.repository.MovimientoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
@RequiredArgsConstructor
public class MovimientoStockController {

    private final MovimientoStockRepository movimientoStockRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MovimientoStockResponse>> listarUltimos(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<MovimientoStockResponse> movimientos = movimientoStockRepository
                .findLatestWithProduct(PageRequest.of(0, safeLimit))
                .stream()
                .map(MovimientoStockResponse::from)
                .toList();
        return ResponseEntity.ok(movimientos);
    }
}
