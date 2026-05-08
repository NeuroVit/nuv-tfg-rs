package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.CrearPedidoProveedorRequest;
import com.nuv.tfgrs.dto.response.PedidoProveedorItemResponse;
import com.nuv.tfgrs.dto.response.PedidoProveedorResponse;
import com.nuv.tfgrs.entity.MovimientoStock;
import com.nuv.tfgrs.entity.Producto;
import com.nuv.tfgrs.entity.Proveedor;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.MovimientoStockRepository;
import com.nuv.tfgrs.repository.ProductoRepository;
import com.nuv.tfgrs.repository.ProveedorRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.service.PedidoProveedorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoProveedorServiceImpl implements PedidoProveedorService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final StockAlertManager stockAlertManager;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional(readOnly = true)
    public List<PedidoProveedorResponse> listarPorProveedor(Long providerId) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        assertProviderExists(providerId, organizationId);
        return fetchOrdersByProvider(providerId, organizationId);
    }

    @Override
    @Transactional
    public PedidoProveedorResponse crearBorrador(Long providerId, CrearPedidoProveedorRequest request) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        Proveedor provider = findProveedorActivoOrThrow(providerId, organizationId);
        validateOrderItems(request.items());

        Map<Long, Producto> productsById = loadProductsMap(request.items().stream()
                .map(CrearPedidoProveedorRequest.Item::productoId)
                .toList(), organizationId);

        for (CrearPedidoProveedorRequest.Item item : request.items()) {
            Producto product = productsById.get(item.productoId());
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más productos no existen");
            }
            if (!Objects.equals(product.getProvider().getId(), providerId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos los productos del pedido deben pertenecer al proveedor seleccionado");
            }
        }

        LocalDate desiredArrival = request.fechaLlegadaDeseada();
        LocalDate recommendedOrderDate = desiredArrival.minusDays(provider.getLeadTimeDays());

        String insertOrderSql = """
                INSERT INTO purchase_orders (
                    organization_id, provider_id, status, desired_arrival_date, recommended_order_date, notes, created_at, updated_at
                )
                VALUES (:organizationId, :providerId, 'DRAFT', :desiredArrivalDate, :recommendedOrderDate, :notes, NOW(), NOW())
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("providerId", providerId)
                .addValue("desiredArrivalDate", Date.valueOf(desiredArrival))
                .addValue("recommendedOrderDate", Date.valueOf(recommendedOrderDate))
                .addValue("notes", normalizeNotes(request.notas()));

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(insertOrderSql, params, keyHolder, new String[]{"id"});
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear el pedido");
        }

        Long orderId = key.longValue();
        insertOrderItems(orderId, request.items(), productsById, organizationId);
        return fetchOrderById(orderId, organizationId);
    }

    @Override
    @Transactional
    public PedidoProveedorResponse enviar(Long orderId) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        String sql = """
                UPDATE purchase_orders
                SET status = 'SENT', sent_at = NOW(), updated_at = NOW()
                WHERE id = :orderId AND organization_id = :organizationId AND status = 'DRAFT'
                """;
        int updated = jdbcTemplate.update(sql, new MapSqlParameterSource("orderId", orderId)
                .addValue("organizationId", organizationId));
        if (updated == 0) {
            throwInvalidStatusTransition(orderId, organizationId, "DRAFT", "No se puede enviar el pedido en su estado actual");
        }
        return fetchOrderById(orderId, organizationId);
    }

    @Override
    @Transactional
    public PedidoProveedorResponse recibir(Long orderId) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        String currentStatus = findOrderStatus(orderId, organizationId);
        if (!"SENT".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden recibir pedidos en estado SENT");
        }

        PedidoProveedorResponse order = fetchOrderById(orderId, organizationId);
        for (PedidoProveedorItemResponse item : order.items()) {
            Producto product = productoRepository.findByIdAndOrganizationId(item.productoId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + item.productoId()));
            int currentQty = product.getCurrentQuantity();
            int incomingQty = item.cantidad();
            int newQty = currentQty + incomingQty;

            BigDecimal previousCost = product.getAcquisitionPrice() != null
                    ? product.getAcquisitionPrice()
                    : product.getUnitPrice();
            BigDecimal incomingCost = item.precioUnitarioSnapshot();

            BigDecimal weightedCost;
            if (newQty <= 0) {
                weightedCost = incomingCost;
            } else {
                BigDecimal totalCostBefore = previousCost.multiply(BigDecimal.valueOf(currentQty));
                BigDecimal totalIncomingCost = incomingCost.multiply(BigDecimal.valueOf(incomingQty));
                weightedCost = totalCostBefore.add(totalIncomingCost)
                        .divide(BigDecimal.valueOf(newQty), 2, RoundingMode.HALF_UP);
            }

            product.setCurrentQuantity(newQty);
            product.setAcquisitionPrice(weightedCost);
            product.setUpdatedAt(LocalDateTime.now());
            Producto saved = productoRepository.save(product);

            MovimientoStock movement = MovimientoStock.builder()
                    .organizationId(organizationId)
                    .product(saved)
                    .delta(item.cantidad())
                    .reason(MovimientoStock.Reason.PURCHASE)
                    .notes("Recepción de pedido proveedor #" + orderId)
                    .movedAt(LocalDateTime.now())
                    .build();
            movimientoStockRepository.save(movement);
            stockAlertManager.syncForProduct(saved);
        }

        String sql = """
                UPDATE purchase_orders
                SET status = 'RECEIVED', received_at = NOW(), updated_at = NOW()
                WHERE id = :orderId AND organization_id = :organizationId AND status = 'SENT'
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("orderId", orderId)
                .addValue("organizationId", organizationId));
        return fetchOrderById(orderId, organizationId);
    }

    @Override
    @Transactional
    public PedidoProveedorResponse cancelar(Long orderId) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        String currentStatus = findOrderStatus(orderId, organizationId);
        if ("RECEIVED".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cancelar un pedido ya recibido");
        }
        if ("CANCELLED".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya está cancelado");
        }

        String sql = """
                UPDATE purchase_orders
                SET status = 'CANCELLED', updated_at = NOW()
                WHERE id = :orderId AND organization_id = :organizationId
                """;
        jdbcTemplate.update(sql, new MapSqlParameterSource("orderId", orderId)
                .addValue("organizationId", organizationId));
        return fetchOrderById(orderId, organizationId);
    }

    private void validateOrderItems(List<CrearPedidoProveedorRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido debe incluir al menos un producto");
        }
        Set<Long> ids = new HashSet<>();
        for (CrearPedidoProveedorRequest.Item item : items) {
            if (!ids.add(item.productoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede repetir un producto dentro del mismo pedido");
            }
        }
    }

    private void insertOrderItems(
            Long orderId,
            List<CrearPedidoProveedorRequest.Item> items,
            Map<Long, Producto> productsById,
            Long organizationId
    ) {
        String sql = """
                INSERT INTO purchase_order_items (order_id, organization_id, product_id, quantity, unit_price_snapshot)
                VALUES (:orderId, :organizationId, :productId, :quantity, :unitPriceSnapshot)
                """;

        MapSqlParameterSource[] batch = items.stream()
                .map(item -> new MapSqlParameterSource()
                        .addValue("orderId", orderId)
                        .addValue("organizationId", organizationId)
                        .addValue("productId", item.productoId())
                        .addValue("quantity", item.cantidad())
                        .addValue("unitPriceSnapshot", resolveOrderUnitCost(item, productsById)))
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
    }

    private BigDecimal resolveOrderUnitCost(
            CrearPedidoProveedorRequest.Item item,
            Map<Long, Producto> productsById
    ) {
        if (item.precioUnitario() != null) {
            return item.precioUnitario();
        }

        Producto product = productsById.get(item.productoId());
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más productos no existen");
        }

        if (product.getAcquisitionPrice() != null) {
            return product.getAcquisitionPrice();
        }
        return product.getUnitPrice();
    }

    private Map<Long, Producto> loadProductsMap(List<Long> productIds, Long organizationId) {
        List<Producto> products = productoRepository.findAllByIdInAndOrganizationId(productIds, organizationId);
        return products.stream().collect(Collectors.toMap(Producto::getId, p -> p));
    }

    private Proveedor findProveedorActivoOrThrow(Long providerId, Long organizationId) {
        Proveedor provider = proveedorRepository.findByIdAndOrganizationId(providerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado: " + providerId));
        if (!Boolean.TRUE.equals(provider.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proveedor no está activo");
        }
        return provider;
    }

    private void assertProviderExists(Long providerId, Long organizationId) {
        if (!proveedorRepository.existsByIdAndOrganizationId(providerId, organizationId)) {
            throw new ResourceNotFoundException("Proveedor no encontrado: " + providerId);
        }
    }

    private String normalizeNotes(String notes) {
        if (notes == null) return null;
        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void throwInvalidStatusTransition(Long orderId, Long organizationId, String expectedStatus, String message) {
        String currentStatus = findOrderStatus(orderId, organizationId);
        if (!expectedStatus.equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    private String findOrderStatus(Long orderId, Long organizationId) {
        String sql = "SELECT status FROM purchase_orders WHERE id = :orderId AND organization_id = :organizationId";
        List<String> rows = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("orderId", orderId)
                .addValue("organizationId", organizationId), String.class);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Pedido no encontrado: " + orderId);
        }
        return rows.get(0);
    }

    private List<PedidoProveedorResponse> fetchOrdersByProvider(Long providerId, Long organizationId) {
        String sql = """
                SELECT
                    po.id,
                    po.provider_id,
                    pr.name AS provider_name,
                    po.status,
                    po.desired_arrival_date,
                    po.recommended_order_date,
                    po.sent_at,
                    po.received_at,
                    po.notes,
                    po.created_at
                FROM purchase_orders po
                JOIN providers pr ON pr.id = po.provider_id
                WHERE po.provider_id = :providerId
                  AND po.organization_id = :organizationId
                ORDER BY po.created_at DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("providerId", providerId)
                .addValue("organizationId", organizationId);
        List<PedidoHeaderRow> headers = jdbcTemplate.query(sql, params, (rs, rowNum) -> new PedidoHeaderRow(
                rs.getLong("id"),
                rs.getLong("provider_id"),
                rs.getString("provider_name"),
                rs.getString("status"),
                rs.getDate("desired_arrival_date").toLocalDate(),
                rs.getDate("recommended_order_date").toLocalDate(),
                toLocalDateTime(rs.getTimestamp("sent_at")),
                toLocalDateTime(rs.getTimestamp("received_at")),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));

        if (headers.isEmpty()) {
            return List.of();
        }

        Map<Long, List<PedidoProveedorItemResponse>> itemsByOrderId = fetchItemsByOrderIds(
                headers.stream().map(PedidoHeaderRow::id).toList(),
                organizationId
        );

        return headers.stream()
                .map(header -> new PedidoProveedorResponse(
                        header.id(),
                        header.providerId(),
                        header.providerName(),
                        header.status(),
                        header.desiredArrivalDate(),
                        header.recommendedOrderDate(),
                        header.sentAt(),
                        header.receivedAt(),
                        header.notes(),
                        header.createdAt(),
                        itemsByOrderId.getOrDefault(header.id(), List.of())
                ))
                .toList();
    }

    private PedidoProveedorResponse fetchOrderById(Long orderId, Long organizationId) {
        String sql = """
                SELECT
                    po.id,
                    po.provider_id,
                    pr.name AS provider_name,
                    po.status,
                    po.desired_arrival_date,
                    po.recommended_order_date,
                    po.sent_at,
                    po.received_at,
                    po.notes,
                    po.created_at
                FROM purchase_orders po
                JOIN providers pr ON pr.id = po.provider_id
                WHERE po.id = :orderId
                  AND po.organization_id = :organizationId
                """;

        List<PedidoHeaderRow> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("orderId", orderId)
                .addValue("organizationId", organizationId), (rs, rowNum) -> new PedidoHeaderRow(
                rs.getLong("id"),
                rs.getLong("provider_id"),
                rs.getString("provider_name"),
                rs.getString("status"),
                rs.getDate("desired_arrival_date").toLocalDate(),
                rs.getDate("recommended_order_date").toLocalDate(),
                toLocalDateTime(rs.getTimestamp("sent_at")),
                toLocalDateTime(rs.getTimestamp("received_at")),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Pedido no encontrado: " + orderId);
        }
        PedidoHeaderRow row = rows.get(0);
        Map<Long, List<PedidoProveedorItemResponse>> itemsByOrderId = fetchItemsByOrderIds(List.of(orderId), organizationId);
        return new PedidoProveedorResponse(
                row.id(),
                row.providerId(),
                row.providerName(),
                row.status(),
                row.desiredArrivalDate(),
                row.recommendedOrderDate(),
                row.sentAt(),
                row.receivedAt(),
                row.notes(),
                row.createdAt(),
                itemsByOrderId.getOrDefault(orderId, List.of())
        );
    }

    private Map<Long, List<PedidoProveedorItemResponse>> fetchItemsByOrderIds(List<Long> orderIds, Long organizationId) {
        String sql = """
                SELECT
                    poi.order_id,
                    poi.product_id,
                    p.name AS product_name,
                    p.sku,
                    poi.quantity,
                    poi.unit_price_snapshot
                FROM purchase_order_items poi
                JOIN products p ON p.id = poi.product_id
                WHERE poi.order_id IN (:orderIds)
                  AND poi.organization_id = :organizationId
                ORDER BY p.name
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("orderIds", orderIds)
                .addValue("organizationId", organizationId);

        return jdbcTemplate.query(sql, params, (rs) -> {
            Map<Long, List<PedidoProveedorItemResponse>> grouped = new HashMap<>();
            while (rs.next()) {
                Long orderId = rs.getLong("order_id");
                grouped.computeIfAbsent(orderId, ignored -> new ArrayList<>()).add(
                        new PedidoProveedorItemResponse(
                                rs.getLong("product_id"),
                                rs.getString("product_name"),
                                rs.getString("sku"),
                                rs.getInt("quantity"),
                                rs.getBigDecimal("unit_price_snapshot")
                        )
                );
            }
            return grouped;
        });
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private record PedidoHeaderRow(
            Long id,
            Long providerId,
            String providerName,
            String status,
            LocalDate desiredArrivalDate,
            LocalDate recommendedOrderDate,
            LocalDateTime sentAt,
            LocalDateTime receivedAt,
            String notes,
            LocalDateTime createdAt
    ) {
    }
}
