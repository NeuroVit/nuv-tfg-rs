package com.nuv.tfgrs.features.reportes.service.impl;

import com.nuv.tfgrs.features.alertas.repository.AlertaStockRepository;
import com.nuv.tfgrs.features.clientes.entity.Cliente;
import com.nuv.tfgrs.features.clientes.repository.ClienteRepository;
import com.nuv.tfgrs.features.inventario.entity.MovimientoStock;
import com.nuv.tfgrs.features.inventario.repository.MovimientoStockRepository;
import com.nuv.tfgrs.features.productos.repository.ProductoRepository;
import com.nuv.tfgrs.features.reportes.dto.request.CrearReporteRequest;
import com.nuv.tfgrs.features.reportes.dto.response.ReporteResponse;
import com.nuv.tfgrs.features.reportes.entity.Reporte;
import com.nuv.tfgrs.features.reportes.repository.ReporteRepository;
import com.nuv.tfgrs.features.reportes.service.ia.ReporteAnalisisContextoOrganizacionalBuilder;
import com.nuv.tfgrs.features.reportes.service.ia.ReporteAnalisisIaEstado;
import com.nuv.tfgrs.features.reportes.service.ia.ReporteAnalisisIaResultado;
import com.nuv.tfgrs.features.reportes.service.ia.ReporteAnalisisIaService;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private AlertaStockRepository alertaStockRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private MovimientoStockRepository movimientoStockRepository;
    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock
    private CurrentOrganizationResolver currentOrganizationResolver;
    @Mock
    private ReporteAnalisisIaService reporteAnalisisIaService;
    @Mock
    private ReporteAnalisisContextoOrganizacionalBuilder contextoBuilder;

    private ReporteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReporteServiceImpl(
                reporteRepository,
                productoRepository,
                alertaStockRepository,
                clienteRepository,
                movimientoStockRepository,
                jdbcTemplate,
                currentOrganizationResolver,
                reporteAnalisisIaService,
                contextoBuilder
        );
        ReflectionTestUtils.setField(service, "reportesAiEnabled", true);
        ReflectionTestUtils.setField(service, "reportesAiTimeoutRaw", "20s");
        ReflectionTestUtils.setField(service, "reportesAiPromptVersion", "v1");
        ReflectionTestUtils.setField(service, "ollamaChatModel", "llama3.2");
    }

    @Test
    void generarDebeGuardarAnalisisIaCuandoOllamaResponde() throws Exception {
        CrearReporteRequest request = buildRequest("MOVIMIENTOS");
        when(movimientoStockRepository.findByOrganizationIdOrderByMovedAtDesc(10L)).thenReturn(List.of());
        when(contextoBuilder.build(10L)).thenReturn(Map.of("ventas", Map.of("totalVentas", 3)));
        when(reporteAnalisisIaService.analizar(
                eq("MOVIMIENTOS"),
                any(String.class),
                eq(Map.of("ventas", Map.of("totalVentas", 3))),
                eq("llama3.2"),
                eq(Duration.ofSeconds(20)),
                eq("v1")
        )).thenReturn(new ReporteAnalisisIaResultado(
                "Resumen IA de prueba.",
                ReporteAnalisisIaEstado.GENERADO,
                "llama3.2",
                LocalDateTime.now(),
                null
        ));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> {
            Reporte r = invocation.getArgument(0);
            r.setId(99L);
            return r;
        });

        ReporteResponse response = service.generar(request, "admin", 10L);

        assertEquals("GENERADO", response.getAnalisisIaEstado());
        assertEquals("Resumen IA de prueba.", response.getAnalisisIa());
        assertEquals("llama3.2", response.getAnalisisIaModelo());
        assertNotNull(response.getId());
    }

    @Test
    void generarDebeGuardarReporteSiOllamaFalla() throws Exception {
        CrearReporteRequest request = buildRequest("MOVIMIENTOS");
        when(movimientoStockRepository.findByOrganizationIdOrderByMovedAtDesc(10L)).thenReturn(List.of(new MovimientoStock()));
        when(contextoBuilder.build(10L)).thenReturn(Map.of());
        when(reporteAnalisisIaService.analizar(
                eq("MOVIMIENTOS"),
                any(String.class),
                eq(Map.of()),
                eq("llama3.2"),
                eq(Duration.ofSeconds(20)),
                eq("v1")
        )).thenThrow(new TimeoutException("timeout IA"));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReporteResponse response = service.generar(request, "admin", 10L);

        assertEquals("NO_DISPONIBLE", response.getAnalisisIaEstado());
        assertTrue(response.getAnalisisIaError().contains("timeout IA"));

        ArgumentCaptor<Reporte> captor = ArgumentCaptor.forClass(Reporte.class);
        verify(reporteRepository).save(captor.capture());
        assertEquals("NO_DISPONIBLE", captor.getValue().getAnalisisIaEstado());
    }

    @Test
    void generarClientesDebeIncluirMetricasComercialesAccionables() throws Exception {
        CrearReporteRequest request = buildRequest("CLIENTES");
        Cliente c1 = new Cliente();
        c1.setName("Cliente A");
        c1.setEmail("a@test.com");
        c1.setActive(true);
        c1.setCreatedAt(LocalDateTime.now().minusDays(5));
        Cliente c2 = new Cliente();
        c2.setName("Cliente B");
        c2.setEmail("b@test.com");
        c2.setActive(true);
        c2.setCreatedAt(LocalDateTime.now().minusDays(40));
        when(clienteRepository.findByOrganizationIdOrderByNameAsc(10L)).thenReturn(List.of(c1, c2));

        when(jdbcTemplate.queryForMap(org.mockito.ArgumentMatchers.contains("clientesConCompras30d"), any(MapSqlParameterSource.class)))
                .thenReturn(Map.of("clientesConCompras30d", 1, "ingresosClientes30d", 2500.75, "ticketPromedio30d", 250.08));
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("COUNT(DISTINCT client_id)"), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(1L);
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("MAX(s.sold_at) AS ultimaCompra"), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(Map.of("nombre", "Cliente A", "totalVentas", 4, "ingresos", 2500.75, "ultimaCompra", "2026-05-17T01:00:00")));
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.contains("HAVING MAX(s.sold_at) IS NULL"), any(MapSqlParameterSource.class)))
                .thenReturn(List.of(Map.of("nombre", "Cliente B", "ultimaCompra", "—", "totalVentasHistoricas", 0)));

        when(contextoBuilder.build(10L)).thenReturn(Map.of());
        when(reporteAnalisisIaService.analizar(
                eq("CLIENTES"),
                any(String.class),
                eq(Map.of()),
                eq("llama3.2"),
                eq(Duration.ofSeconds(20)),
                eq("v1")
        )).thenReturn(new ReporteAnalisisIaResultado(
                "Analisis clientes generado.",
                ReporteAnalisisIaEstado.GENERADO,
                "llama3.2",
                LocalDateTime.now(),
                null
        ));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> {
            Reporte r = invocation.getArgument(0);
            r.setId(77L);
            return r;
        });

        ReporteResponse response = service.generar(request, "admin", 10L);

        assertEquals("GENERADO", response.getAnalisisIaEstado());
        assertTrue(response.getDatos().contains("clientesConCompras30Dias"));
        assertTrue(response.getDatos().contains("ingresosClientes30Dias"));
        assertTrue(response.getDatos().contains("topClientes30Dias"));
        assertTrue(response.getDatos().contains("clientesSinCompraReciente"));
    }

    private CrearReporteRequest buildRequest(String tipo) {
        CrearReporteRequest request = new CrearReporteRequest();
        request.setNombre("Reporte test");
        request.setTipo(tipo);
        request.setDescripcion("desc");
        return request;
    }
}
