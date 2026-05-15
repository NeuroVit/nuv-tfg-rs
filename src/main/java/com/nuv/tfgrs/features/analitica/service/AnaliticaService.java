package com.nuv.tfgrs.features.analitica.service;

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

import java.util.List;

public interface AnaliticaService {
    List<InventorySummaryRowResponse> inventarioResumen();
    PagedResponse<InventorySummaryRowResponse> inventarioResumenPaginado(int limit, int offset);
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
    PagedResponse<RentabilidadProductoRowResponse> rentabilidadProductosPaginado(int days, int limit, int offset);
    List<BundlePerformanceRowResponse> bundlesPerformance(int days, int limit);
    List<ClienteRentabilidadRowResponse> clientesRentabilidad(int days, int limit);
    List<ProveedorPerformanceRowResponse> proveedoresPerformance(int days, int limit);
    List<AlertaSerieRowResponse> alertasSerie(int days);
    List<ProductCreationRowResponse> productosCreadosTimeline(int days);
    List<StockReceiptRowResponse> stockRecibidoTimeline(int days);
    AnaliticaMetaResponse metadata(int days);
}
