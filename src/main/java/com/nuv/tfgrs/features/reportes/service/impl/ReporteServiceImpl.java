package com.nuv.tfgrs.features.reportes.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuv.tfgrs.features.reportes.dto.request.CrearReporteRequest;
import com.nuv.tfgrs.features.reportes.dto.response.ReporteResponse;
import com.nuv.tfgrs.features.alertas.entity.AlertaStock;
import com.nuv.tfgrs.features.clientes.entity.Cliente;
import com.nuv.tfgrs.features.inventario.entity.MovimientoStock;
import com.nuv.tfgrs.features.productos.entity.Producto;
import com.nuv.tfgrs.features.reportes.entity.Reporte;
import com.nuv.tfgrs.features.alertas.repository.AlertaStockRepository;
import com.nuv.tfgrs.features.clientes.repository.ClienteRepository;
import com.nuv.tfgrs.features.inventario.repository.MovimientoStockRepository;
import com.nuv.tfgrs.features.productos.repository.ProductoRepository;
import com.nuv.tfgrs.features.reportes.repository.ReporteRepository;
import com.nuv.tfgrs.features.reportes.service.ReporteService;
import com.nuv.tfgrs.features.reportes.service.ia.ReporteAnalisisContextoOrganizacionalBuilder;
import com.nuv.tfgrs.features.reportes.service.ia.ReporteAnalisisIaEstado;
import com.nuv.tfgrs.features.reportes.service.ia.ReporteAnalisisIaResultado;
import com.nuv.tfgrs.features.reportes.service.ia.ReporteAnalisisIaService;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteServiceImpl implements ReporteService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReporteRepository reporteRepository;
    private final ProductoRepository productoRepository;
    private final AlertaStockRepository alertaStockRepository;
    private final ClienteRepository clienteRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentOrganizationResolver currentOrganizationResolver;
    private final ReporteAnalisisIaService reporteAnalisisIaService;
    private final ReporteAnalisisContextoOrganizacionalBuilder contextoOrganizacionalBuilder;

    @Value("${reportes.ai.enabled:true}")
    private boolean reportesAiEnabled;

    @Value("${reportes.ai.timeout:20s}")
    private String reportesAiTimeoutRaw;

    @Value("${reportes.ai.prompt-version:v1}")
    private String reportesAiPromptVersion;

    @Value("${spring.ai.model.chat:ollama}")
    private String chatProvider;

    @Value("${spring.ai.ollama.chat.model:${spring.ai.ollama.chat.options.model:llama3.2}}")
    private String ollamaChatModel;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private String openAiChatModel;

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> listar() {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        return reporteRepository.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> listarPorTipo(String tipo) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        return reporteRepository.findByOrganizationIdAndTipoOrderByCreatedAtDesc(organizationId, tipo)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReporteResponse generar(CrearReporteRequest request, String username) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        return generar(request, username, organizationId);
    }

    @Override
    @Transactional
    public ReporteResponse generar(CrearReporteRequest request, String username, Long organizationId) {
        String datos = generarDatos(request.getTipo(), organizationId);
        ReporteAnalisisIaResultado analisisIa = generarAnalisisIa(request.getTipo(), datos, organizationId);

        Reporte reporte = Reporte.builder()
                .organizationId(organizationId)
                .nombre(request.getNombre())
                .tipo(request.getTipo())
                .descripcion(request.getDescripcion())
                .datos(datos)
                .analisisIa(analisisIa.analisis())
                .analisisIaEstado(analisisIa.estado().name())
                .analisisIaModelo(analisisIa.modelo())
                .analisisIaGeneradoEn(analisisIa.generadoEn())
                .analisisIaError(analisisIa.error())
                .generadoPor(username)
                .build();

        return toResponse(reporteRepository.save(reporte));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        if (!reporteRepository.existsByIdAndOrganizationId(id, organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado");
        }
        Reporte reporte = reporteRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));
        reporteRepository.delete(reporte);
    }

    private String generarDatos(String tipo, Long organizationId) {
        try {
            Map<String, Object> datos = new HashMap<>();
            datos.put("generadoEn", LocalDateTime.now().toString());

            switch (tipo) {
                case "INVENTARIO" -> {
                    List<Producto> productos = productoRepository.findByOrganizationId(organizationId);
                    List<Producto> stockBajo = productoRepository.findLowStockProductsByOrganizationId(organizationId);
                    double valorTotal = productos.stream()
                            .mapToDouble(p -> (p.getUnitPrice() != null ? p.getUnitPrice().doubleValue() : 0)
                                    * p.getCurrentQuantity())
                            .sum();
                    datos.put("totalProductos", productos.size());
                    datos.put("valorTotalInventario", Math.round(valorTotal * 100.0) / 100.0);
                    datos.put("productosStockBajo", stockBajo.size());
                    datos.put("resumenStockBajo", stockBajo.stream().map(p -> Map.of(
                            "nombre", p.getName(),
                            "sku", p.getSku(),
                            "stockActual", p.getCurrentQuantity(),
                            "stockMinimo", p.getMinimumStockLevel()
                    )).collect(Collectors.toList()));
                }
                case "ALERTAS" -> {
                    List<AlertaStock> alertas = alertaStockRepository.findByOrganizationId(organizationId);
                    List<AlertaStock> pendientes = alertas.stream()
                            .filter(a -> !Boolean.TRUE.equals(a.getResolved())).collect(Collectors.toList());
                    datos.put("totalAlertas", alertas.size());
                    datos.put("alertasPendientes", pendientes.size());
                    datos.put("alertasResueltas", alertas.size() - pendientes.size());
                    datos.put("detalle", pendientes.stream().map(a -> Map.of(
                            "producto", a.getProduct() != null ? a.getProduct().getName() : "—",
                            "sku", a.getProduct() != null ? a.getProduct().getSku() : "—",
                            "tipo", a.getAlertType() != null ? a.getAlertType().name() : "LOW_STOCK"
                    )).collect(Collectors.toList()));
                }
                case "CLIENTES" -> {
                    List<Cliente> clientes = clienteRepository.findByOrganizationIdOrderByNameAsc(organizationId);
                    MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);

                    Map<String, Object> ventas30d = jdbcTemplate.queryForMap(
                            """
                                    SELECT
                                      COUNT(DISTINCT client_id) AS clientesConCompras30d,
                                      COALESCE(SUM(total_amount),0) AS ingresosClientes30d,
                                      COALESCE(AVG(total_amount),0) AS ticketPromedio30d
                                    FROM sales
                                    WHERE organization_id = :organizationId
                                      AND client_id IS NOT NULL
                                      AND sold_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                                    """,
                            params
                    );

                    Long clientesConComprasHistorico = jdbcTemplate.queryForObject(
                            """
                                    SELECT COUNT(DISTINCT client_id)
                                    FROM sales
                                    WHERE organization_id = :organizationId
                                      AND client_id IS NOT NULL
                                    """,
                            params, Long.class
                    );

                    List<Map<String, Object>> topClientes30d = jdbcTemplate.queryForList(
                            """
                                    SELECT
                                      c.name AS nombre,
                                      COUNT(s.id) AS totalVentas,
                                      ROUND(SUM(s.total_amount),2) AS ingresos,
                                      MAX(s.sold_at) AS ultimaCompra
                                    FROM sales s
                                    JOIN clients c ON c.id = s.client_id
                                    WHERE s.organization_id = :organizationId
                                      AND c.organization_id = :organizationId
                                      AND s.sold_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
                                    GROUP BY c.id, c.name
                                    ORDER BY ingresos DESC
                                    LIMIT 5
                                    """,
                            params
                    );

                    List<Map<String, Object>> clientesSinCompraReciente = jdbcTemplate.queryForList(
                            """
                                    SELECT
                                      c.name AS nombre,
                                      MAX(s.sold_at) AS ultimaCompra,
                                      COUNT(s.id) AS totalVentasHistoricas
                                    FROM clients c
                                    LEFT JOIN sales s
                                      ON s.client_id = c.id
                                     AND s.organization_id = :organizationId
                                    WHERE c.organization_id = :organizationId
                                      AND c.is_active = true
                                    GROUP BY c.id, c.name
                                    HAVING MAX(s.sold_at) IS NULL
                                       OR MAX(s.sold_at) < DATE_SUB(NOW(), INTERVAL 90 DAY)
                                    ORDER BY ultimaCompra ASC
                                    LIMIT 5
                                    """,
                            params
                    );

                    long totalClientes = clientes.size();
                    long activos = clientes.stream().filter(c -> Boolean.TRUE.equals(c.getActive())).count();
                    long clientesConCompras30d = toLong(ventas30d.get("clientesConCompras30d"));
                    long historicos = clientesConComprasHistorico != null ? clientesConComprasHistorico : 0L;
                    long sinComprasHistorico = Math.max(0L, totalClientes - historicos);
                    long sinCompras30d = Math.max(0L, totalClientes - clientesConCompras30d);

                    datos.put("totalClientes", totalClientes);
                    datos.put("clientesActivos", activos);
                    datos.put("clientesArchivados", totalClientes - activos);
                    datos.put("nuevosUltimos30Dias", clientes.stream()
                            .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30)))
                            .count());
                    datos.put("clientesConComprasHistorico", historicos);
                    datos.put("clientesSinComprasHistorico", sinComprasHistorico);
                    datos.put("clientesConCompras30Dias", clientesConCompras30d);
                    datos.put("clientesSinCompras30Dias", sinCompras30d);
                    datos.put("ingresosClientes30Dias", Math.round(toDouble(ventas30d.get("ingresosClientes30d")) * 100.0) / 100.0);
                    datos.put("ticketPromedioClientes30Dias", Math.round(toDouble(ventas30d.get("ticketPromedio30d")) * 100.0) / 100.0);
                    datos.put("topClientes30Dias", topClientes30d.stream().map(r -> Map.of(
                            "nombre", r.get("nombre"),
                            "totalVentas", toLong(r.get("totalVentas")),
                            "ingresos", Math.round(toDouble(r.get("ingresos")) * 100.0) / 100.0,
                            "ultimaCompra", r.get("ultimaCompra") != null ? r.get("ultimaCompra").toString() : "—"
                    )).collect(Collectors.toList()));
                    datos.put("clientesSinCompraReciente", clientesSinCompraReciente.stream().map(r -> Map.of(
                            "nombre", r.get("nombre"),
                            "ultimaCompra", r.get("ultimaCompra") != null ? r.get("ultimaCompra").toString() : "—",
                            "totalVentasHistoricas", toLong(r.get("totalVentasHistoricas"))
                    )).collect(Collectors.toList()));
                    datos.put("listado", clientes.stream().limit(15).map(c -> Map.of(
                            "nombre", c.getName(),
                            "email", c.getEmail() != null ? c.getEmail() : "—",
                            "activo", Boolean.TRUE.equals(c.getActive())
                    )).collect(Collectors.toList()));
                }
                case "MOVIMIENTOS" -> {
                    List<MovimientoStock> movimientos = movimientoStockRepository.findByOrganizationIdOrderByMovedAtDesc(organizationId);
                    long entradas = movimientos.stream().filter(m -> m.getDelta() > 0).count();
                    long salidas  = movimientos.stream().filter(m -> m.getDelta() < 0).count();
                    datos.put("totalMovimientos", movimientos.size());
                    datos.put("entradas", entradas);
                    datos.put("salidas", salidas);
                }
                case "VENTAS" -> {
                    MapSqlParameterSource params = new MapSqlParameterSource("organizationId", organizationId);

                    Map<String, Object> totales = jdbcTemplate.queryForMap(
                            "SELECT COUNT(*) AS totalVentas, COALESCE(SUM(total_amount),0) AS totalIngresos, " +
                            "COALESCE(AVG(total_amount),0) AS promedioVenta " +
                            "FROM sales WHERE organization_id = :organizationId",
                            params);

                    Map<String, Object> mes = jdbcTemplate.queryForMap(
                            "SELECT COUNT(*) AS ventasMes, COALESCE(SUM(total_amount),0) AS ingresosMes " +
                            "FROM sales " +
                            "WHERE organization_id = :organizationId " +
                            "AND YEAR(sold_at)=YEAR(NOW()) AND MONTH(sold_at)=MONTH(NOW())",
                            params);

                    Long nuevosClientes = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM clients " +
                            "WHERE organization_id = :organizationId " +
                            "AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                            params, Long.class);

                    List<Map<String, Object>> topRows = jdbcTemplate.queryForList(
                            "SELECT p.name AS nombre, SUM(si.quantity) AS totalUnidades, " +
                            "ROUND(SUM(si.unit_price_snapshot * si.quantity),2) AS ingresos " +
                            "FROM sale_items si " +
                            "JOIN sales s ON s.id = si.sale_id " +
                            "JOIN products p ON p.id = si.product_id " +
                            "WHERE s.organization_id = :organizationId " +
                            "AND p.organization_id = :organizationId " +
                            "GROUP BY si.product_id, p.name ORDER BY ingresos DESC LIMIT 5",
                            params);

                    List<Map<String, Object>> topClientes = jdbcTemplate.queryForList(
                            "SELECT c.name AS nombre, COUNT(s.id) AS totalVentas, " +
                            "ROUND(SUM(s.total_amount),2) AS totalGastado " +
                            "FROM sales s JOIN clients c ON c.id = s.client_id " +
                            "WHERE s.organization_id = :organizationId " +
                            "AND c.organization_id = :organizationId " +
                            "GROUP BY s.client_id, c.name ORDER BY totalGastado DESC LIMIT 3",
                            params);

                    datos.put("totalVentas", toLong(totales.get("totalVentas")));
                    datos.put("totalIngresos", toDouble(totales.get("totalIngresos")));
                    datos.put("promedioVenta", Math.round(toDouble(totales.get("promedioVenta")) * 100.0) / 100.0);
                    datos.put("ventasMes", toLong(mes.get("ventasMes")));
                    datos.put("ingresosMes", toDouble(mes.get("ingresosMes")));
                    datos.put("nuevosClientes30Dias", nuevosClientes != null ? nuevosClientes : 0);
                    datos.put("topProductos", topRows.stream().map(r -> Map.of(
                            "nombre", r.get("nombre"),
                            "totalUnidades", toLong(r.get("totalUnidades")),
                            "ingresos", toDouble(r.get("ingresos"))
                    )).collect(Collectors.toList()));
                    datos.put("topClientes", topClientes.stream().map(r -> Map.of(
                            "nombre", r.get("nombre"),
                            "totalVentas", toLong(r.get("totalVentas")),
                            "totalGastado", toDouble(r.get("totalGastado"))
                    )).collect(Collectors.toList()));
                }
                default -> datos.put("error", "Tipo de reporte no reconocido");
            }

            return MAPPER.writeValueAsString(datos);
        } catch (Exception e) {
            return "{\"error\": \"No se pudo generar el reporte\"}";
        }
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private ReporteAnalisisIaResultado generarAnalisisIa(String tipoReporte, String datosReporteJson, Long organizationId) {
        if (!reportesAiEnabled) {
            return new ReporteAnalisisIaResultado(
                    null,
                    ReporteAnalisisIaEstado.NO_DISPONIBLE,
                    resolveActiveModelName(),
                    null,
                    "Análisis IA deshabilitado por configuración"
            );
        }

        try {
            Map<String, Object> contexto = contextoOrganizacionalBuilder.build(organizationId);
            return reporteAnalisisIaService.analizar(
                    tipoReporte,
                    datosReporteJson,
                    contexto,
                    resolveActiveModelName(),
                    parseTimeout(),
                    reportesAiPromptVersion
            );
        } catch (Exception ex) {
            Throwable root = rootCause(ex);
            ReporteAnalisisIaEstado estado = isIaUnavailable(root)
                    ? ReporteAnalisisIaEstado.NO_DISPONIBLE
                    : ReporteAnalisisIaEstado.ERROR;
            log.warn(
                    "No se pudo generar analisis IA para tipo={} orgId={} estado={} causa={}",
                    tipoReporte,
                    organizationId,
                    estado,
                    formatError(root)
            );
            return new ReporteAnalisisIaResultado(
                    null,
                    estado,
                    resolveActiveModelName(),
                    null,
                    truncateError(formatError(root))
            );
        }
    }

    private Duration parseTimeout() {
        return DurationStyle.detectAndParse(reportesAiTimeoutRaw);
    }

    private String truncateError(String message) {
        String safe = message == null || message.isBlank() ? "Error desconocido al generar analisis IA" : message;
        return safe.length() <= 500 ? safe : safe.substring(0, 500);
    }

    private String formatError(Throwable throwable) {
        if (throwable == null) {
            return "Error desconocido al generar analisis IA";
        }
        String className = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return className;
        }
        return className + ": " + message;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private boolean isIaUnavailable(Throwable throwable) {
        return throwable instanceof TimeoutException
                || throwable instanceof ResourceAccessException
                || throwable instanceof java.net.ConnectException
                || (throwable.getMessage() != null
                && throwable.getMessage().toLowerCase().contains("connection refused"));
    }

    private String resolveActiveModelName() {
        if ("openai".equalsIgnoreCase(chatProvider)) {
            return openAiChatModel;
        }
        return ollamaChatModel;
    }

    private ReporteResponse toResponse(Reporte r) {
        return ReporteResponse.builder()
                .id(r.getId())
                .nombre(r.getNombre())
                .tipo(r.getTipo())
                .descripcion(r.getDescripcion())
                .datos(r.getDatos())
                .analisisIa(r.getAnalisisIa())
                .analisisIaEstado(r.getAnalisisIaEstado())
                .analisisIaModelo(r.getAnalisisIaModelo())
                .analisisIaGeneradoEn(r.getAnalisisIaGeneradoEn())
                .analisisIaError(r.getAnalisisIaError())
                .generadoPor(r.getGeneradoPor())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
