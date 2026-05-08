package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.CrearProveedorRequest;
import com.nuv.tfgrs.dto.request.EditarProveedorRequest;
import com.nuv.tfgrs.dto.response.PedidoProveedorResumenResponse;
import com.nuv.tfgrs.dto.response.ProveedorDetalleResponse;
import com.nuv.tfgrs.dto.response.ProveedorKpiResponse;
import com.nuv.tfgrs.dto.response.ProveedorProductoResumenResponse;
import com.nuv.tfgrs.dto.response.ProveedorResponse;
import com.nuv.tfgrs.entity.Proveedor;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.ProductoRepository;
import com.nuv.tfgrs.repository.ProveedorRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar(boolean includeArchived) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        List<Proveedor> proveedores = includeArchived
                ? proveedorRepository.findByOrganizationIdOrderByNameAsc(organizationId)
                : proveedorRepository.findByOrganizationIdAndActiveTrueOrderByNameAsc(organizationId);

        return proveedores.stream()
                .map(proveedor -> ProveedorResponse.from(
                        proveedor,
                        productoRepository.countByProviderIdAndOrganizationId(proveedor.getId(), organizationId)
                ))
                .toList();
    }

    @Override
    @Transactional
    public ProveedorResponse crear(CrearProveedorRequest request) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        String nombre = normalizeRequired(request.nombre(), "El nombre del proveedor es obligatorio");
        if (proveedorRepository.existsByNameIgnoreCaseAndOrganizationId(nombre, organizationId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proveedor con ese nombre");
        }

        Proveedor proveedor = Proveedor.builder()
                .organizationId(organizationId)
                .name(nombre)
                .warehouseLocation(normalizeRequired(request.ubicacionAlmacen(), "La ubicación del almacén es obligatoria"))
                .leadTimeDays(request.diasEntrega())
                .active(true)
                .build();

        Proveedor saved = proveedorRepository.save(proveedor);
        return ProveedorResponse.from(saved, 0);
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, EditarProveedorRequest request) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Proveedor proveedor = findByIdOrThrow(id, organizationId);
        String nombre = normalizeRequired(request.nombre(), "El nombre del proveedor es obligatorio");
        if (proveedorRepository.existsByNameIgnoreCaseAndOrganizationIdAndIdNot(nombre, organizationId, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proveedor con ese nombre");
        }

        proveedor.setName(nombre);
        proveedor.setWarehouseLocation(normalizeRequired(
                request.ubicacionAlmacen(),
                "La ubicación del almacén es obligatoria"
        ));
        proveedor.setLeadTimeDays(request.diasEntrega());

        Proveedor saved = proveedorRepository.save(proveedor);
        return ProveedorResponse.from(saved, productoRepository.countByProviderIdAndOrganizationId(saved.getId(), organizationId));
    }

    @Override
    @Transactional
    public ProveedorResponse archivar(Long id) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Proveedor proveedor = findByIdOrThrow(id, organizationId);
        proveedor.setActive(false);
        proveedor.setDeletedAt(LocalDateTime.now());
        Proveedor saved = proveedorRepository.save(proveedor);
        return ProveedorResponse.from(saved, productoRepository.countByProviderIdAndOrganizationId(saved.getId(), organizationId));
    }

    @Override
    @Transactional
    public ProveedorResponse restaurar(Long id) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Proveedor proveedor = findByIdOrThrow(id, organizationId);
        proveedor.setActive(true);
        proveedor.setDeletedAt(null);
        Proveedor saved = proveedorRepository.save(proveedor);
        return ProveedorResponse.from(saved, productoRepository.countByProviderIdAndOrganizationId(saved.getId(), organizationId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorDetalleResponse detalle(Long id) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Proveedor proveedor = findByIdOrThrow(id, organizationId);
        ProveedorResponse providerResponse = ProveedorResponse.from(
                proveedor,
                productoRepository.countByProviderIdAndOrganizationId(proveedor.getId(), organizationId)
        );

        List<ProveedorProductoResumenResponse> productos = productoRepository
                .findByProviderIdAndOrganizationIdOrderByNameAsc(proveedor.getId(), organizationId)
                .stream()
                .map(producto -> new ProveedorProductoResumenResponse(
                        producto.getId(),
                        producto.getName(),
                        producto.getSku(),
                        producto.getCurrentQuantity(),
                        producto.getMinimumStockLevel(),
                        producto.getCurrentQuantity() <= producto.getMinimumStockLevel()
                ))
                .toList();

        List<PedidoProveedorResumenResponse> pedidosRecientes = listarPedidosRecientes(proveedor.getId(), organizationId);
        ProveedorKpiResponse kpis = calcularKpis(proveedor.getId(), organizationId);

        return new ProveedorDetalleResponse(providerResponse, kpis, productos, pedidosRecientes);
    }

    private Proveedor findByIdOrThrow(Long id, Long organizationId) {
        return proveedorRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + id));
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private List<PedidoProveedorResumenResponse> listarPedidosRecientes(Long providerId, Long organizationId) {
        String sql = """
                SELECT
                    po.id,
                    po.status,
                    po.desired_arrival_date,
                    po.recommended_order_date,
                    po.sent_at,
                    po.received_at,
                    po.created_at,
                    COUNT(poi.product_id) AS total_items,
                    COALESCE(SUM(poi.quantity), 0) AS total_unidades,
                    COALESCE(SUM(poi.quantity * poi.unit_price_snapshot), 0) AS importe_estimado
                FROM purchase_orders po
                LEFT JOIN purchase_order_items poi ON poi.order_id = po.id
                WHERE po.provider_id = :providerId
                  AND po.organization_id = :organizationId
                GROUP BY po.id, po.status, po.desired_arrival_date, po.recommended_order_date, po.sent_at, po.received_at, po.created_at
                ORDER BY po.created_at DESC
                LIMIT 20
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("providerId", providerId)
                .addValue("organizationId", organizationId);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new PedidoProveedorResumenResponse(
                rs.getLong("id"),
                rs.getString("status"),
                rs.getDate("desired_arrival_date").toLocalDate(),
                rs.getDate("recommended_order_date").toLocalDate(),
                rs.getTimestamp("sent_at") != null ? rs.getTimestamp("sent_at").toLocalDateTime() : null,
                rs.getTimestamp("received_at") != null ? rs.getTimestamp("received_at").toLocalDateTime() : null,
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getInt("total_items"),
                rs.getInt("total_unidades"),
                rs.getDouble("importe_estimado")
        ));
    }

    private ProveedorKpiResponse calcularKpis(Long providerId, Long organizationId) {
        String sql = """
                SELECT
                    COUNT(*) AS total_pedidos,
                    SUM(CASE WHEN status IN ('DRAFT', 'SENT') THEN 1 ELSE 0 END) AS pedidos_activos,
                    SUM(CASE WHEN status = 'RECEIVED' THEN 1 ELSE 0 END) AS pedidos_recibidos,
                    SUM(CASE WHEN status <> 'CANCELLED' THEN 1 ELSE 0 END) AS pedidos_no_cancelados,
                    SUM(CASE WHEN status = 'RECEIVED' AND received_at <= TIMESTAMP(desired_arrival_date, '23:59:59') THEN 1 ELSE 0 END) AS recibidos_en_fecha
                FROM purchase_orders
                WHERE provider_id = :providerId
                  AND organization_id = :organizationId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("providerId", providerId)
                .addValue("organizationId", organizationId);
        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
            long total = rs.getLong("total_pedidos");
            long activos = rs.getLong("pedidos_activos");
            long recibidos = rs.getLong("pedidos_recibidos");
            long noCancelados = rs.getLong("pedidos_no_cancelados");
            long recibidosEnFecha = rs.getLong("recibidos_en_fecha");

            double fillRate = noCancelados == 0 ? 0 : round2((double) recibidos / noCancelados);
            double onTimeRate = recibidos == 0 ? 0 : round2((double) recibidosEnFecha / recibidos);

            return new ProveedorKpiResponse(total, activos, recibidos, fillRate, onTimeRate);
        });
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
