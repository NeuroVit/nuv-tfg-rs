package com.nuv.tfgrs.features.reportes.service.ia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteAnalisisContextoOrganizacionalBuilderTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void buildDebeConsultarSiempreConOrganizationId() {
        ReporteAnalisisContextoOrganizacionalBuilder builder = new ReporteAnalisisContextoOrganizacionalBuilder(jdbcTemplate);

        when(jdbcTemplate.queryForMap(contains("FROM products"), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("totalProductos", 1, "unidadesTotales", 10, "valorInventario", 120.0, "productosStockBajo", 0));
        when(jdbcTemplate.queryForMap(contains("FROM stock_alerts"), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("totalAlertas", 2, "pendientes", 1, "resueltas", 1));
        when(jdbcTemplate.queryForMap(contains("FROM clients"), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("totalClientes", 4, "activos", 3, "archivados", 1, "nuevos30d", 1));
        when(jdbcTemplate.queryForMap(contains("FROM stock_movements"), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("totalMovimientos", 7, "entradas", 4, "salidas", 3, "ultimos30d", 5));
        when(jdbcTemplate.queryForMap(contains("FROM sales"), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("totalVentas", 11, "totalIngresos", 500.0, "ticketPromedio", 45.5, "ventas30d", 5, "ingresos30d", 200.0));
        when(jdbcTemplate.queryForList(contains("DATE_FORMAT(sold_at"), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(Map.of("mes", "2026-05", "totalVentas", 2, "ingresos", 100.0)));
        when(jdbcTemplate.queryForList(contains("FROM sale_items"), any(MapSqlParameterSource.class)))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("JOIN clients c"), any(MapSqlParameterSource.class)))
                .thenReturn(List.of());

        Map<String, Object> contexto = builder.build(123L);

        assertNotNull(contexto.get("inventario"));
        assertNotNull(contexto.get("ventas"));

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate, atLeastOnce()).queryForMap(any(String.class), captor.capture());
        captor.getAllValues().forEach(params ->
                assertEquals(123L, params.getValue("organizationId"))
        );
    }
}
