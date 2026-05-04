package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.response.BundleCandidateRowResponse;
import com.nuv.tfgrs.dto.response.FinanzasResumenResponse;
import com.nuv.tfgrs.dto.response.InventorySummaryRowResponse;
import com.nuv.tfgrs.dto.response.PriceTrendRowResponse;
import com.nuv.tfgrs.dto.response.RentabilidadProductoRowResponse;
import com.nuv.tfgrs.dto.response.SalesVelocityRowResponse;
import com.nuv.tfgrs.dto.response.StockDistributionRowResponse;
import com.nuv.tfgrs.dto.response.StockPorCategoriaRowResponse;
import com.nuv.tfgrs.dto.response.TopProductoVelocidadRowResponse;
import com.nuv.tfgrs.dto.response.VelocidadResumenRowResponse;
import com.nuv.tfgrs.service.AnaliticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analitica")
@RequiredArgsConstructor
public class AnaliticaController {

    private final AnaliticaService analiticaService;

    @GetMapping("/inventario")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InventorySummaryRowResponse>> inventario() {
        return ResponseEntity.ok(analiticaService.inventarioResumen());
    }

    @GetMapping("/velocidad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalesVelocityRowResponse>> velocidad(
            @RequestParam(name = "limit", defaultValue = "120") int limit,
            @RequestParam(name = "days", required = false) Integer days
    ) {
        return ResponseEntity.ok(analiticaService.velocidadVentas(limit, days));
    }

    @GetMapping("/precios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PriceTrendRowResponse>> precios(
            @RequestParam(name = "limit", defaultValue = "120") int limit
    ) {
        return ResponseEntity.ok(analiticaService.tendenciasPrecios(limit));
    }

    @GetMapping("/bundles-candidatos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BundleCandidateRowResponse>> bundles(
            @RequestParam(name = "limit", defaultValue = "120") int limit
    ) {
        return ResponseEntity.ok(analiticaService.candidatosBundle(limit));
    }

    @GetMapping("/velocidad-resumen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VelocidadResumenRowResponse>> velocidadResumen(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analiticaService.velocidadResumen(days));
    }

    @GetMapping("/top-productos-velocidad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TopProductoVelocidadRowResponse>> topProductosVelocidad(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(analiticaService.topProductosVelocidad(days, limit));
    }

    @GetMapping("/distribucion-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StockDistributionRowResponse>> distribucionStock() {
        return ResponseEntity.ok(analiticaService.distribucionStock());
    }

    @GetMapping("/stock-por-categoria")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StockPorCategoriaRowResponse>> stockPorCategoria() {
        return ResponseEntity.ok(analiticaService.stockPorCategoria());
    }

    @GetMapping("/finanzas-resumen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FinanzasResumenResponse> finanzasResumen(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analiticaService.finanzasResumen(days));
    }

    @GetMapping("/rentabilidad-productos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RentabilidadProductoRowResponse>> rentabilidadProductos(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "limit", defaultValue = "12") int limit
    ) {
        return ResponseEntity.ok(analiticaService.rentabilidadProductos(days, limit));
    }
}
