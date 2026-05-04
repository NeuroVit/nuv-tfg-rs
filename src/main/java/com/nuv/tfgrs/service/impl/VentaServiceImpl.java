package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.CrearVentaRequest;
import com.nuv.tfgrs.dto.response.VentaItemResponse;
import com.nuv.tfgrs.dto.response.VentaResponse;
import com.nuv.tfgrs.entity.MovimientoStock;
import com.nuv.tfgrs.entity.Producto;
import com.nuv.tfgrs.entity.Usuario;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.ClienteRepository;
import com.nuv.tfgrs.repository.MovimientoStockRepository;
import com.nuv.tfgrs.repository.ProductoRepository;
import com.nuv.tfgrs.repository.UsuarioRepository;
import com.nuv.tfgrs.service.VentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> listar(Long clientId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<VentaHeaderRow> headers = fetchSalesHeaders(clientId, safeLimit);
        if (headers.isEmpty()) {
            return List.of();
        }

        Map<Long, List<VentaItemResponse>> itemsBySaleId = fetchSaleItems(headers.stream().map(VentaHeaderRow::id).toList());

        return headers.stream().map(header -> new VentaResponse(
                header.id,
                header.clientId,
                header.clientName,
                header.soldAt,
                header.notes,
                header.totalAmount,
                header.createdAt,
                itemsBySaleId.getOrDefault(header.id, List.of())
        )).toList();
    }

    @Override
    @Transactional
    public VentaResponse crear(CrearVentaRequest request, String actorUsername) {
        if (!clienteRepository.existsById(request.clientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente no encontrado");
        }

        List<CrearVentaRequest.Item> requestItems = request.items();
        if (requestItems == null || requestItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La venta debe incluir al menos un producto");
        }

        Set<Long> uniqueProductIds = new HashSet<>();
        for (CrearVentaRequest.Item item : requestItems) {
            if (!uniqueProductIds.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede repetir un producto en la venta");
            }
        }

        Map<Long, Producto> productsById = productoRepository.findAllById(uniqueProductIds)
                .stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        if (productsById.size() != uniqueProductIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más productos no existen");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CrearVentaRequest.Item item : requestItems) {
            Producto product = productsById.get(item.productId());
            if (product.getCurrentQuantity() < item.quantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente para " + product.getName() + ". Disponible=" + product.getCurrentQuantity()
                );
            }
            total = total.add(product.getUnitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        Long createdBy = usuarioRepository.findByUsername(actorUsername).map(Usuario::getId).orElse(null);
        Long saleId = insertSale(request.clientId(), request.notas(), total, createdBy);
        insertSaleItems(saleId, requestItems, productsById);

        for (CrearVentaRequest.Item item : requestItems) {
            Producto product = productsById.get(item.productId());
            product.setCurrentQuantity(product.getCurrentQuantity() - item.quantity());
            product.setUpdatedAt(LocalDateTime.now());
            Producto saved = productoRepository.save(product);

            MovimientoStock movement = MovimientoStock.builder()
                    .product(saved)
                    .delta(-item.quantity())
                    .reason(MovimientoStock.Reason.SALE)
                    .notes("Venta #" + saleId)
                    .movedAt(LocalDateTime.now())
                    .build();
            movimientoStockRepository.save(movement);
        }

        return fetchSaleById(saleId);
    }

    private Long insertSale(Long clientId, String notes, BigDecimal total, Long createdBy) {
        String sql = """
                INSERT INTO sales (client_id, sold_at, notes, total_amount, created_by, created_at)
                VALUES (:clientId, NOW(), :notes, :totalAmount, :createdBy, NOW())
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("clientId", clientId)
                .addValue("notes", normalizeNotes(notes))
                .addValue("totalAmount", total)
                .addValue("createdBy", createdBy), keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear la venta");
        }
        return key.longValue();
    }

    private void insertSaleItems(Long saleId, List<CrearVentaRequest.Item> items, Map<Long, Producto> productsById) {
        String sql = """
                INSERT INTO sale_items (sale_id, product_id, quantity, unit_price_snapshot, cost_price_snapshot)
                VALUES (:saleId, :productId, :quantity, :price, :cost)
                """;

        MapSqlParameterSource[] batch = items.stream()
                .map(item -> new MapSqlParameterSource()
                        .addValue("saleId", saleId)
                        .addValue("productId", item.productId())
                        .addValue("quantity", item.quantity())
                        .addValue("price", productsById.get(item.productId()).getUnitPrice())
                        .addValue("cost", resolveUnitCost(productsById.get(item.productId())))
                )
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
    }

    private BigDecimal resolveUnitCost(Producto product) {
        if (product.getAcquisitionPrice() != null) {
            return product.getAcquisitionPrice();
        }
        return product.getUnitPrice();
    }

    private List<VentaHeaderRow> fetchSalesHeaders(Long clientId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    s.id,
                    s.client_id,
                    c.name AS client_name,
                    s.sold_at,
                    s.notes,
                    s.total_amount,
                    s.created_at
                FROM sales s
                JOIN clients c ON c.id = s.client_id
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (clientId != null) {
            sql.append(" WHERE s.client_id = :clientId ");
            params.addValue("clientId", clientId);
        }

        sql.append(" ORDER BY s.sold_at DESC LIMIT :limit");
        params.addValue("limit", limit);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new VentaHeaderRow(
                rs.getLong("id"),
                rs.getLong("client_id"),
                rs.getString("client_name"),
                rs.getTimestamp("sold_at").toLocalDateTime(),
                rs.getString("notes"),
                rs.getBigDecimal("total_amount"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ));
    }

    private VentaResponse fetchSaleById(Long saleId) {
        String sql = """
                SELECT
                    s.id,
                    s.client_id,
                    c.name AS client_name,
                    s.sold_at,
                    s.notes,
                    s.total_amount,
                    s.created_at
                FROM sales s
                JOIN clients c ON c.id = s.client_id
                WHERE s.id = :saleId
                """;

        List<VentaHeaderRow> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("saleId", saleId), (rs, rowNum) ->
                new VentaHeaderRow(
                        rs.getLong("id"),
                        rs.getLong("client_id"),
                        rs.getString("client_name"),
                        rs.getTimestamp("sold_at").toLocalDateTime(),
                        rs.getString("notes"),
                        rs.getBigDecimal("total_amount"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                )
        );

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Venta no encontrada: " + saleId);
        }

        VentaHeaderRow row = rows.get(0);
        Map<Long, List<VentaItemResponse>> itemsBySale = fetchSaleItems(List.of(saleId));
        return new VentaResponse(
                row.id,
                row.clientId,
                row.clientName,
                row.soldAt,
                row.notes,
                row.totalAmount,
                row.createdAt,
                itemsBySale.getOrDefault(saleId, List.of())
        );
    }

    private Map<Long, List<VentaItemResponse>> fetchSaleItems(List<Long> saleIds) {
        if (saleIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT
                    si.sale_id,
                    si.product_id,
                    p.name AS product_name,
                    p.sku,
                    si.quantity,
                    si.unit_price_snapshot
                FROM sale_items si
                JOIN products p ON p.id = si.product_id
                WHERE si.sale_id IN (:saleIds)
                ORDER BY p.name
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("saleIds", saleIds));
        Map<Long, List<VentaItemResponse>> grouped = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Long saleId = ((Number) row.get("sale_id")).longValue();
            Long productId = ((Number) row.get("product_id")).longValue();
            Integer quantity = ((Number) row.get("quantity")).intValue();
            BigDecimal unitPrice = (BigDecimal) row.get("unit_price_snapshot");

            grouped.computeIfAbsent(saleId, ignored -> new ArrayList<>()).add(new VentaItemResponse(
                    productId,
                    (String) row.get("product_name"),
                    (String) row.get("sku"),
                    quantity,
                    unitPrice,
                    unitPrice.multiply(BigDecimal.valueOf(quantity))
            ));
        }

        return grouped;
    }

    private String normalizeNotes(String notes) {
        if (notes == null) return null;
        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record VentaHeaderRow(
            Long id,
            Long clientId,
            String clientName,
            LocalDateTime soldAt,
            String notes,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
    }
}
