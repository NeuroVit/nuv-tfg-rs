package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.response.BundleCandidateRowResponse;
import com.nuv.tfgrs.dto.response.BundlePerformanceRowResponse;
import com.nuv.tfgrs.dto.response.ClienteRentabilidadRowResponse;
import com.nuv.tfgrs.dto.response.FinanzasResumenResponse;
import com.nuv.tfgrs.dto.response.InventorySummaryRowResponse;
import com.nuv.tfgrs.dto.response.AlertaSerieRowResponse;
import com.nuv.tfgrs.dto.response.AnaliticaMetaResponse;
import com.nuv.tfgrs.dto.response.PriceTrendRowResponse;
import com.nuv.tfgrs.dto.response.ProductCreationRowResponse;
import com.nuv.tfgrs.dto.response.ProveedorPerformanceRowResponse;
import com.nuv.tfgrs.dto.response.RentabilidadProductoRowResponse;
import com.nuv.tfgrs.dto.response.RevenueComparativaResponse;
import com.nuv.tfgrs.dto.response.SalesVelocityRowResponse;
import com.nuv.tfgrs.dto.response.StockDistributionRowResponse;
import com.nuv.tfgrs.dto.response.StockPorCategoriaRowResponse;
import com.nuv.tfgrs.dto.response.StockReceiptRowResponse;
import com.nuv.tfgrs.dto.response.TopProductoVelocidadRowResponse;
import com.nuv.tfgrs.dto.response.VelocidadResumenRowResponse;

import java.util.List;

public interface AnaliticaService {
    List<InventorySummaryRowResponse> inventarioResumen();
    List<SalesVelocityRowResponse> velocidadVentas(int limit, Integer days);
    List<PriceTrendRowResponse> tendenciasPrecios(int limit, Integer days);
    List<BundleCandidateRowResponse> candidatosBundle(int limit, Integer days);
    List<VelocidadResumenRowResponse> velocidadResumen(int days);
    List<TopProductoVelocidadRowResponse> topProductosVelocidad(int days, int limit);
    List<StockDistributionRowResponse> distribucionStock();
    List<StockPorCategoriaRowResponse> stockPorCategoria();
    FinanzasResumenResponse finanzasResumen(int days);
    RevenueComparativaResponse revenueComparativa(int days);
    List<RentabilidadProductoRowResponse> rentabilidadProductos(int days, int limit);
    List<BundlePerformanceRowResponse> bundlesPerformance(int days, int limit);
    List<ClienteRentabilidadRowResponse> clientesRentabilidad(int days, int limit);
    List<ProveedorPerformanceRowResponse> proveedoresPerformance(int days, int limit);
    List<AlertaSerieRowResponse> alertasSerie(int days);
    List<ProductCreationRowResponse> productosCreadosTimeline(int days);
    List<StockReceiptRowResponse> stockRecibidoTimeline(int days);
    AnaliticaMetaResponse metadata(int days);
}
