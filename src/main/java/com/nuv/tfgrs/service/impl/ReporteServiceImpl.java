package com.nuv.tfgrs.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuv.tfgrs.dto.request.CrearReporteRequest;
import com.nuv.tfgrs.dto.response.ReporteResponse;
import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.entity.Cliente;
import com.nuv.tfgrs.entity.MovimientoStock;
import com.nuv.tfgrs.entity.Producto;
import com.nuv.tfgrs.entity.Reporte;
import com.nuv.tfgrs.repository.AlertaStockRepository;
import com.nuv.tfgrs.repository.ClienteRepository;
import com.nuv.tfgrs.repository.MovimientoStockRepository;
import com.nuv.tfgrs.repository.ProductoRepository;
import com.nuv.tfgrs.repository.ReporteRepository;
import com.nuv.tfgrs.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ReporteRepository reporteRepository;
    private final ProductoRepository productoRepository;
    private final AlertaStockRepository alertaStockRepository;
    private final ClienteRepository clienteRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> listar() {
        return reporteRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> listarPorTipo(String tipo) {
        return reporteRepository.findByTipoOrderByCreatedAtDesc(tipo)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReporteResponse generar(CrearReporteRequest request, String username) {
        String datos = generarDatos(request.getTipo());

        Reporte reporte = Reporte.builder()
                .nombre(request.getNombre())
                .tipo(request.getTipo())
                .descripcion(request.getDescripcion())
                .datos(datos)
                .generadoPor(username)
                .build();

        return toResponse(reporteRepository.save(reporte));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!reporteRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado");
        }
        reporteRepository.deleteById(id);
    }

    private String generarDatos(String tipo) {
        try {
            Map<String, Object> datos = new HashMap<>();
            datos.put("generadoEn", LocalDateTime.now().toString());

            switch (tipo) {
                case "INVENTARIO" -> {
                    List<Producto> productos = productoRepository.findAll();
                    List<Producto> stockBajo = productoRepository.findLowStockProducts();
                    double valorTotal = productos.stream()
                            .mapToDouble(p -> (p.getAcquisitionPrice() != null ? p.getAcquisitionPrice().doubleValue() : 0)
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
                    List<AlertaStock> alertas = alertaStockRepository.findAll();
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
                    List<Cliente> clientes = clienteRepository.findAll();
                    long activos = clientes.stream().filter(c -> Boolean.TRUE.equals(c.getActive())).count();

                    Long nuevos30d = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM clients WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                            new MapSqlParameterSource(), Long.class);

                    datos.put("totalClientes", clientes.size());
                    datos.put("clientesActivos", activos);
                    datos.put("clientesArchivados", clientes.size() - activos);
                    datos.put("nuevosUltimos30Dias", nuevos30d != null ? nuevos30d : 0);
                    datos.put("listado", clientes.stream().map(c -> Map.of(
                            "nombre", c.getName(),
                            "email", c.getEmail() != null ? c.getEmail() : "—",
                            "activo", Boolean.TRUE.equals(c.getActive())
                    )).collect(Collectors.toList()));
                }
                case "MOVIMIENTOS" -> {
                    List<MovimientoStock> movimientos = movimientoStockRepository.findAll();
                    long entradas = movimientos.stream().filter(m -> m.getDelta() > 0).count();
                    long salidas  = movimientos.stream().filter(m -> m.getDelta() < 0).count();
                    datos.put("totalMovimientos", movimientos.size());
                    datos.put("entradas", entradas);
                    datos.put("salidas", salidas);
                }
                case "VENTAS" -> {
                    MapSqlParameterSource empty = new MapSqlParameterSource();

                    // Global totals
                    Map<String, Object> totales = jdbcTemplate.queryForMap(
                            "SELECT COUNT(*) AS totalVentas, COALESCE(SUM(total_amount),0) AS totalIngresos, " +
                            "COALESCE(AVG(total_amount),0) AS promedioVenta FROM sales",
                            empty);

                    // Current month
                    Map<String, Object> mes = jdbcTemplate.queryForMap(
                            "SELECT COUNT(*) AS ventasMes, COALESCE(SUM(total_amount),0) AS ingresosMes " +
                            "FROM sales WHERE YEAR(sold_at)=YEAR(NOW()) AND MONTH(sold_at)=MONTH(NOW())",
                            empty);

                    // New clients last 30 days
                    Long nuevosClientes = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM clients WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)",
                            empty, Long.class);

                    // Top 5 products by revenue
                    List<Map<String, Object>> topRows = jdbcTemplate.queryForList(
                            "SELECT p.name AS nombre, SUM(si.quantity) AS totalUnidades, " +
                            "ROUND(SUM(si.unit_price_snapshot * si.quantity),2) AS ingresos " +
                            "FROM sale_items si JOIN products p ON p.id = si.product_id " +
                            "GROUP BY si.product_id, p.name ORDER BY ingresos DESC LIMIT 5",
                            empty);

                    // Top client by total purchases
                    List<Map<String, Object>> topClientes = jdbcTemplate.queryForList(
                            "SELECT c.name AS nombre, COUNT(s.id) AS totalVentas, " +
                            "ROUND(SUM(s.total_amount),2) AS totalGastado " +
                            "FROM sales s JOIN clients c ON c.id = s.client_id " +
                            "GROUP BY s.client_id, c.name ORDER BY totalGastado DESC LIMIT 3",
                            empty);

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

    private ReporteResponse toResponse(Reporte r) {
        return ReporteResponse.builder()
                .id(r.getId())
                .nombre(r.getNombre())
                .tipo(r.getTipo())
                .descripcion(r.getDescripcion())
                .datos(r.getDatos())
                .generadoPor(r.getGeneradoPor())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
