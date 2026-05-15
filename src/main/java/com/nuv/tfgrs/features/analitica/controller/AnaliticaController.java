package com.nuv.tfgrs.features.analitica.controller;

import com.nuv.tfgrs.features.bundles.dto.response.BundleCandidateRowResponse;
import com.nuv.tfgrs.features.bundles.dto.response.BundlePerformanceRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.ClienteRentabilidadRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.FinanzasResumenResponse;
import com.nuv.tfgrs.features.analitica.dto.response.InventorySummaryRowResponse;
import com.nuv.tfgrs.features.alertas.dto.response.AlertaSerieRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.AnaliticaMetaResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.analitica.dto.response.PriceTrendRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.ProductCreationRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.ProveedorPerformanceRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.RentabilidadProductoRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.RevenueComparativaResponse;
import com.nuv.tfgrs.features.analitica.dto.response.SalesVelocityRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.StockDistributionRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.StockPorCategoriaRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.StockReceiptRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.TopProductoVelocidadRowResponse;
import com.nuv.tfgrs.features.analitica.dto.response.VelocidadResumenRowResponse;
import com.nuv.tfgrs.features.analitica.service.AnaliticaService;
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

    @GetMapping("/inventario/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<InventorySummaryRowResponse>> inventarioPaginado(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset
    ) {
        return ResponseEntity.ok(analiticaService.inventarioResumenPaginado(limit, offset));
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
            @RequestParam(name = "limit", defaultValue = "120") int limit,
            @RequestParam(name = "days", required = false) Integer days
    ) {
        return ResponseEntity.ok(analiticaService.tendenciasPrecios(limit, days));
    }

    @GetMapping("/bundles-candidatos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BundleCandidateRowResponse>> bundles(
            @RequestParam(name = "limit", defaultValue = "120") int limit,
            @RequestParam(name = "days", required = false) Integer days
    ) {
        return ResponseEntity.ok(analiticaService.candidatosBundle(limit, days));
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

    @GetMapping("/revenue-comparativa")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RevenueComparativaResponse> revenueComparativa(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analiticaService.revenueComparativa(days));
    }

    @GetMapping("/rentabilidad-productos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RentabilidadProductoRowResponse>> rentabilidadProductos(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "limit", defaultValue = "12") int limit
    ) {
        return ResponseEntity.ok(analiticaService.rentabilidadProductos(days, limit));
    }

    @GetMapping("/rentabilidad-productos/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<RentabilidadProductoRowResponse>> rentabilidadProductosPaginado(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset
    ) {
        return ResponseEntity.ok(analiticaService.rentabilidadProductosPaginado(days, limit, offset));
    }

    @GetMapping("/bundles-performance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BundlePerformanceRowResponse>> bundlesPerformance(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(analiticaService.bundlesPerformance(days, limit));
    }

    @GetMapping("/clientes-rentabilidad")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClienteRentabilidadRowResponse>> clientesRentabilidad(
            @RequestParam(name = "days", defaultValue = "30") int days,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(analiticaService.clientesRentabilidad(days, limit));
    }

    @GetMapping("/proveedores-performance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProveedorPerformanceRowResponse>> proveedoresPerformance(
            @RequestParam(name = "days", defaultValue = "90") int days,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(analiticaService.proveedoresPerformance(days, limit));
    }

    @GetMapping("/alertas-serie")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AlertaSerieRowResponse>> alertasSerie(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analiticaService.alertasSerie(days));
    }

    @GetMapping("/productos-creados")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductCreationRowResponse>> productosCreados(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analiticaService.productosCreadosTimeline(days));
    }

    @GetMapping("/stock-recibido")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StockReceiptRowResponse>> stockRecibido(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analiticaService.stockRecibidoTimeline(days));
    }

    @GetMapping("/meta")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnaliticaMetaResponse> metadata(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analiticaService.metadata(days));
    }
}
