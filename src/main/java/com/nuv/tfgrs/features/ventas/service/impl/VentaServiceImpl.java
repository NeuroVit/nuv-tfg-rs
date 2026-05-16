package com.nuv.tfgrs.features.ventas.service.impl;

import com.nuv.tfgrs.features.ventas.dto.request.CrearVentaRequest;
import com.nuv.tfgrs.features.alertas.service.impl.StockAlertManager;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.ventas.dto.response.VentaItemResponse;
import com.nuv.tfgrs.features.ventas.dto.response.VentaResponse;
import com.nuv.tfgrs.features.inventario.entity.MovimientoStock;
import com.nuv.tfgrs.features.productos.entity.Producto;
import com.nuv.tfgrs.features.usuarios.entity.Usuario;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.features.ventas.domain.VentaPricingPolicy;
import com.nuv.tfgrs.features.ventas.domain.VentaRequestValidator;
import com.nuv.tfgrs.features.ventas.infrastructure.VentaSalesQueryRepository;
import com.nuv.tfgrs.features.clientes.repository.ClienteRepository;
import com.nuv.tfgrs.features.inventario.repository.MovimientoStockRepository;
import com.nuv.tfgrs.features.productos.repository.ProductoRepository;
import com.nuv.tfgrs.features.usuarios.repository.UsuarioRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.features.ventas.service.VentaEmailService;
import com.nuv.tfgrs.features.ventas.service.VentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaServiceImpl implements VentaService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final UsuarioRepository usuarioRepository;
    private final StockAlertManager stockAlertManager;
    private final CurrentOrganizationResolver currentOrganizationResolver;
    private final VentaEmailService ventaEmailService;
    private final VentaPricingPolicy pricingPolicy;
    private final VentaRequestValidator requestValidator;
    private final VentaSalesQueryRepository salesQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponse> listar(Long clientId, int limit) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<VentaHeaderRow> headers = fetchSalesHeaders(clientId, safeLimit, organizationId);
        if (headers.isEmpty()) {
            return List.of();
        }

        Map<Long, List<VentaItemResponse>> itemsBySaleId = fetchSaleItems(headers.stream().map(VentaHeaderRow::id).toList(), organizationId);

        return headers.stream().map(header -> new VentaResponse(
                header.id,
                header.clientId,
                header.clientName,
                header.soldAt,
                header.notes,
                header.subtotalAmount,
                header.discountTotalAmount,
                header.globalDiscountPct,
                header.totalAmount,
                header.createdAt,
                itemsBySaleId.getOrDefault(header.id, List.of())
        )).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<VentaResponse> listarPaginado(
            Long clientId,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            Integer limit,
            Integer offset
    ) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 200));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);

        long total = countSalesHeaders(clientId, search, fromDate, toDate, organizationId);
        if (total == 0) {
            return new PagedResponse<>(List.of(), total, safeLimit, safeOffset);
        }

        List<VentaHeaderRow> headers = fetchSalesHeadersPaged(
                clientId,
                search,
                fromDate,
                toDate,
                safeLimit,
                safeOffset,
                organizationId
        );
        Map<Long, List<VentaItemResponse>> itemsBySaleId = fetchSaleItems(headers.stream().map(VentaHeaderRow::id).toList(), organizationId);

        List<VentaResponse> content = headers.stream().map(header -> new VentaResponse(
                header.id,
                header.clientId,
                header.clientName,
                header.soldAt,
                header.notes,
                header.subtotalAmount,
                header.discountTotalAmount,
                header.globalDiscountPct,
                header.totalAmount,
                header.createdAt,
                itemsBySaleId.getOrDefault(header.id, List.of())
        )).toList();

        return new PagedResponse<>(content, total, safeLimit, safeOffset);
    }

    @Override
    @Transactional
    public VentaResponse crear(CrearVentaRequest request, String actorUsername) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        if (!clienteRepository.existsByIdAndOrganizationIdAndActiveTrue(request.clientId(), organizationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente no encontrado o inactivo");
        }

        List<CrearVentaRequest.Item> directItems = request.items() == null ? List.of() : request.items();
        List<CrearVentaRequest.BundleItem> bundleItems = request.bundles() == null ? List.of() : request.bundles();

        if (directItems.isEmpty() && bundleItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La venta debe incluir al menos un producto o un bundle");
        }

        validateNoRepeatedProductsInDirectItems(directItems);
        validateNoRepeatedBundles(bundleItems);

        Map<Long, BundleDefinition> bundlesById = loadBundleDefinitions(bundleItems, organizationId);

        Set<Long> allProductIds = new HashSet<>();
        directItems.forEach(item -> allProductIds.add(item.productId()));
        bundlesById.values().forEach(bundle -> bundle.components().forEach(component -> allProductIds.add(component.productId())));

        Map<Long, Producto> productsById = productoRepository.findAllByIdInAndOrganizationId(allProductIds, organizationId)
                .stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        if (productsById.size() != allProductIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más productos no existen");
        }

        BigDecimal globalDiscountPct = normalizePct(request.descuentoPctGlobal());
        List<LineCandidate> candidates = new ArrayList<>();

        for (CrearVentaRequest.Item item : directItems) {
            Producto product = productsById.get(item.productId());
            BigDecimal lineDiscountPct = normalizePct(item.descuentoPct());
            candidates.add(new LineCandidate(
                    product.getId(),
                    item.quantity(),
                    product.getUnitPrice(),
                    lineDiscountPct,
                    resolveUnitCost(product),
                    null
            ));
        }

        for (CrearVentaRequest.BundleItem bundleItem : bundleItems) {
            BundleDefinition bundle = bundlesById.get(bundleItem.bundleId());
            if (bundle == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bundle no encontrado: " + bundleItem.bundleId());
            }

            BigDecimal bundleDiscountPct = bundleItem.descuentoPctOverride() != null
                    ? normalizePct(bundleItem.descuentoPctOverride())
                    : normalizePct(bundle.discountPct());

            for (BundleComponent component : bundle.components()) {
                Producto product = productsById.get(component.productId());
                int quantity = Math.multiplyExact(bundleItem.quantity(), component.quantity());
                candidates.add(new LineCandidate(
                        product.getId(),
                        quantity,
                        product.getUnitPrice(),
                        bundleDiscountPct,
                        resolveUnitCost(product),
                        bundle.id()
                ));
            }
        }

        Map<Long, ProductSaleAccumulator> saleByProduct = aggregateCandidates(candidates, globalDiscountPct);
        validateStockAvailability(saleByProduct, productsById);

        List<SaleItemInsert> saleItemsToInsert = buildSaleItemsForInsert(saleByProduct);
        BigDecimal subtotalAmount = sumListAmounts(saleItemsToInsert);
        BigDecimal totalAmount = sumNetAmounts(saleItemsToInsert);
        BigDecimal discountTotalAmount = subtotalAmount.subtract(totalAmount).setScale(2, RoundingMode.HALF_UP);

        Long createdBy = usuarioRepository.findByUsername(actorUsername).map(Usuario::getId).orElse(null);
        Long saleId = insertSale(
                organizationId,
                request.clientId(),
                request.notas(),
                subtotalAmount,
                discountTotalAmount,
                globalDiscountPct,
                totalAmount,
                createdBy
        );

        insertSaleItems(saleId, saleItemsToInsert, organizationId);
        updateStockAndCreateMovements(saleId, saleByProduct, productsById, organizationId);

        VentaResponse response = fetchSaleById(saleId, organizationId);
        clienteRepository.findByIdAndOrganizationId(request.clientId(), organizationId).ifPresent(cliente ->
                CompletableFuture.runAsync(() -> {
                    try {
                        ventaEmailService.enviarComprobanteVenta(response, cliente);
                    } catch (Exception ex) {
                        log.warn("No se pudo enviar comprobante de venta #{} al cliente {}", saleId, cliente.getId(), ex);
                    }
                })
        );
        return response;
    }

    private Map<Long, ProductSaleAccumulator> aggregateCandidates(
            List<LineCandidate> candidates,
            BigDecimal globalDiscountPct
    ) {
        Map<Long, ProductSaleAccumulator> byProduct = new LinkedHashMap<>();

        for (LineCandidate candidate : candidates) {
            BigDecimal unitAfterItemDiscount = applyDiscount(candidate.unitListPrice(), candidate.lineDiscountPct());
            BigDecimal unitAfterGlobalDiscount = applyDiscount(unitAfterItemDiscount, globalDiscountPct);

            BigDecimal qty = BigDecimal.valueOf(candidate.quantity());
            BigDecimal listAmount = candidate.unitListPrice().multiply(qty);
            BigDecimal netAmount = unitAfterGlobalDiscount.multiply(qty);

            ProductSaleAccumulator current = byProduct.computeIfAbsent(
                    candidate.productId(),
                    ignored -> new ProductSaleAccumulator(
                            candidate.productId(),
                            candidate.unitListPrice(),
                            candidate.unitCost(),
                            candidate.bundleId()
                    )
            );

            if (!Objects.equals(current.bundleId, candidate.bundleId())) {
                current.bundleId = null;
            }
            current.quantity += candidate.quantity();
            current.listAmount = current.listAmount.add(listAmount);
            current.netAmount = current.netAmount.add(netAmount);
        }

        return byProduct;
    }

    private void validateStockAvailability(
            Map<Long, ProductSaleAccumulator> saleByProduct,
            Map<Long, Producto> productsById
    ) {
        for (ProductSaleAccumulator row : saleByProduct.values()) {
            Producto product = productsById.get(row.productId);
            if (product.getCurrentQuantity() < row.quantity) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente para " + product.getName() + ". Disponible=" + product.getCurrentQuantity()
                );
            }
        }
    }

    private List<SaleItemInsert> buildSaleItemsForInsert(Map<Long, ProductSaleAccumulator> saleByProduct) {
        List<SaleItemInsert> result = new ArrayList<>();

        for (ProductSaleAccumulator row : saleByProduct.values()) {
            BigDecimal qty = BigDecimal.valueOf(row.quantity);
            BigDecimal listUnitPrice = row.unitListPrice.setScale(2, RoundingMode.HALF_UP);
            BigDecimal netUnitPrice = row.netAmount.divide(qty, 2, RoundingMode.HALF_UP);

            // Keep totals aligned with what is persisted/displayed per line (qty * rounded unit price).
            BigDecimal listAmountRounded = listUnitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netAmountRounded = netUnitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal discountAmount = listAmountRounded.subtract(netAmountRounded);
            BigDecimal discountPct = safePercent(discountAmount, listAmountRounded);

            result.add(new SaleItemInsert(
                    row.productId,
                    row.bundleId,
                    row.quantity,
                    listUnitPrice,
                    discountPct,
                    netUnitPrice,
                    row.unitCost,
                    listAmountRounded,
                    netAmountRounded
            ));
        }

        return result;
    }

    private BigDecimal sumListAmounts(List<SaleItemInsert> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleItemInsert row : rows) {
            total = total.add(row.listAmount());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumNetAmounts(List<SaleItemInsert> rows) {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleItemInsert row : rows) {
            total = total.add(row.netAmount());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void updateStockAndCreateMovements(
            Long saleId,
            Map<Long, ProductSaleAccumulator> saleByProduct,
            Map<Long, Producto> productsById,
            Long organizationId
    ) {
        for (ProductSaleAccumulator row : saleByProduct.values()) {
            Producto product = productsById.get(row.productId);
            product.setCurrentQuantity(product.getCurrentQuantity() - row.quantity);
            product.setUpdatedAt(LocalDateTime.now());
            Producto saved = productoRepository.save(product);

            MovimientoStock movement = MovimientoStock.builder()
                    .organizationId(organizationId)
                    .product(saved)
                    .delta(-row.quantity)
                    .reason(MovimientoStock.Reason.SALE)
                    .notes("Venta #" + saleId)
                    .movedAt(LocalDateTime.now())
                    .build();
            movimientoStockRepository.save(movement);
            try {
                stockAlertManager.syncForProduct(saved);
            } catch (Exception ex) {
                log.warn("No se pudieron sincronizar alertas para producto {} en venta #{}", saved.getId(), saleId, ex);
            }
        }
    }

    private Long insertSale(
            Long organizationId,
            Long clientId,
            String notes,
            BigDecimal subtotalAmount,
            BigDecimal discountTotalAmount,
            BigDecimal globalDiscountPct,
            BigDecimal total,
            Long createdBy
    ) {
        String sql = """
                INSERT INTO sales (
                    organization_id,
                    client_id,
                    sold_at,
                    notes,
                    subtotal_amount,
                    discount_total_amount,
                    global_discount_pct,
                    total_amount,
                    created_by,
                    created_at
                )
                VALUES (
                    :organizationId,
                    :clientId,
                    NOW(),
                    :notes,
                    :subtotalAmount,
                    :discountTotalAmount,
                    :globalDiscountPct,
                    :totalAmount,
                    :createdBy,
                    NOW()
                )
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("organizationId", organizationId)
                .addValue("clientId", clientId)
                .addValue("notes", normalizeNotes(notes))
                .addValue("subtotalAmount", subtotalAmount)
                .addValue("discountTotalAmount", discountTotalAmount)
                .addValue("globalDiscountPct", globalDiscountPct)
                .addValue("totalAmount", total)
                .addValue("createdBy", createdBy), keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo crear la venta");
        }
        return key.longValue();
    }

    private void insertSaleItems(Long saleId, List<SaleItemInsert> rows, Long organizationId) {
        String sql = """
                INSERT INTO sale_items (
                    sale_id,
                    organization_id,
                    product_id,
                    bundle_id,
                    quantity,
                    list_unit_price_snapshot,
                    discount_pct_snapshot,
                    unit_price_snapshot,
                    cost_price_snapshot
                )
                VALUES (
                    :saleId,
                    :organizationId,
                    :productId,
                    :bundleId,
                    :quantity,
                    :listUnitPrice,
                    :discountPct,
                    :netUnitPrice,
                    :unitCost
                )
                """;

        MapSqlParameterSource[] batch = rows.stream()
                .map(row -> new MapSqlParameterSource()
                        .addValue("saleId", saleId)
                        .addValue("organizationId", organizationId)
                        .addValue("productId", row.productId())
                        .addValue("bundleId", row.bundleId())
                        .addValue("quantity", row.quantity())
                        .addValue("listUnitPrice", row.listUnitPrice())
                        .addValue("discountPct", row.discountPct())
                        .addValue("netUnitPrice", row.netUnitPrice())
                        .addValue("unitCost", row.unitCost())
                )
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
    }

    private Map<Long, BundleDefinition> loadBundleDefinitions(List<CrearVentaRequest.BundleItem> bundleItems, Long organizationId) {
        if (bundleItems.isEmpty()) {
            return Map.of();
        }

        Set<Long> bundleIds = bundleItems.stream().map(CrearVentaRequest.BundleItem::bundleId).collect(Collectors.toSet());

        String sql = """
                SELECT
                    b.id AS bundle_id,
                    b.name AS bundle_name,
                    b.discount_pct,
                    b.active,
                    bi.product_id,
                    bi.quantity
                FROM bundles b
                LEFT JOIN bundle_items bi ON bi.bundle_id = b.id
                WHERE b.id IN (:bundleIds)
                  AND b.organization_id = :organizationId
                ORDER BY b.id
                """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, new MapSqlParameterSource("bundleIds", bundleIds)
                .addValue("organizationId", organizationId));

        Map<Long, MutableBundleDefinition> mutable = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long bundleId = asLong(row.get("bundle_id"));
            MutableBundleDefinition current = mutable.computeIfAbsent(
                    bundleId,
                    ignored -> new MutableBundleDefinition(
                            bundleId,
                            (String) row.get("bundle_name"),
                            asBigDecimal(row.get("discount_pct")),
                            asBoolean(row.get("active"))
                    )
            );

            Long productId = asLong(row.get("product_id"));
            Integer quantity = asInteger(row.get("quantity"));
            if (productId != null && quantity != null) {
                if (quantity <= 0) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "El bundle contiene una cantidad inválida para el producto " + productId
                    );
                }
                current.components.add(new BundleComponent(productId, quantity));
            }
        }

        for (Long requestedId : bundleIds) {
            if (!mutable.containsKey(requestedId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bundle no encontrado: " + requestedId);
            }
        }

        Map<Long, BundleDefinition> result = new LinkedHashMap<>();
        for (MutableBundleDefinition item : mutable.values()) {
            if (!item.active) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El bundle está inactivo: " + item.name);
            }
            if (item.components.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El bundle no contiene productos: " + item.name);
            }
            result.put(item.id, new BundleDefinition(item.id, item.name, item.discountPct, List.copyOf(item.components)));
        }

        return result;
    }

    private void validateNoRepeatedProductsInDirectItems(List<CrearVentaRequest.Item> items) {
        requestValidator.validateNoRepeatedProductsInDirectItems(items);
    }

    private void validateNoRepeatedBundles(List<CrearVentaRequest.BundleItem> bundles) {
        requestValidator.validateNoRepeatedBundles(bundles);
    }

    private BigDecimal applyDiscount(BigDecimal amount, BigDecimal discountPct) {
        return pricingPolicy.applyDiscount(amount, discountPct);
    }

    private BigDecimal normalizePct(BigDecimal pct) {
        return pricingPolicy.normalizePct(pct);
    }

    private BigDecimal safePercent(BigDecimal value, BigDecimal base) {
        return pricingPolicy.safePercent(value, base);
    }

    private BigDecimal resolveUnitCost(Producto product) {
        return pricingPolicy.resolveUnitCost(product);
    }

    private List<VentaHeaderRow> fetchSalesHeaders(Long clientId, int limit, Long organizationId) {
        return salesQueryRepository.fetchSalesHeaders(clientId, limit, organizationId)
                .stream()
                .map(this::toVentaHeaderRow)
                .toList();
    }

    private List<VentaHeaderRow> fetchSalesHeadersPaged(
            Long clientId,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            int limit,
            int offset,
            Long organizationId
    ) {
        return salesQueryRepository.fetchSalesHeadersPaged(clientId, search, fromDate, toDate, limit, offset, organizationId)
                .stream()
                .map(this::toVentaHeaderRow)
                .toList();
    }

    private long countSalesHeaders(
            Long clientId,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            Long organizationId
    ) {
        return salesQueryRepository.countSalesHeaders(clientId, search, fromDate, toDate, organizationId);
    }


    private VentaResponse fetchSaleById(Long saleId, Long organizationId) {
        VentaSalesQueryRepository.VentaHeaderRow header = salesQueryRepository.fetchSaleHeaderById(saleId, organizationId);
        if (header == null) {
            throw new ResourceNotFoundException("Venta no encontrada: " + saleId);
        }

        VentaHeaderRow row = toVentaHeaderRow(header);
        Map<Long, List<VentaItemResponse>> itemsBySale = fetchSaleItems(List.of(saleId), organizationId);
        return new VentaResponse(
                row.id,
                row.clientId,
                row.clientName,
                row.soldAt,
                row.notes,
                row.subtotalAmount,
                row.discountTotalAmount,
                row.globalDiscountPct,
                row.totalAmount,
                row.createdAt,
                itemsBySale.getOrDefault(saleId, List.of())
        );
    }

    private Map<Long, List<VentaItemResponse>> fetchSaleItems(List<Long> saleIds, Long organizationId) {
        return salesQueryRepository.fetchSaleItems(saleIds, organizationId);
    }

    private VentaHeaderRow toVentaHeaderRow(VentaSalesQueryRepository.VentaHeaderRow row) {
        return new VentaHeaderRow(
                row.id(),
                row.clientId(),
                row.clientName(),
                row.soldAt(),
                row.notes(),
                row.subtotalAmount(),
                row.discountTotalAmount(),
                row.globalDiscountPct(),
                row.totalAmount(),
                row.createdAt()
        );
    }

    private String normalizeNotes(String notes) {
        if (notes == null) return null;
        String trimmed = notes.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return BigDecimal.ZERO;
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        return null;
    }

    private Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return null;
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number number) return number.intValue() != 0;
        if (value instanceof String text) return "true".equalsIgnoreCase(text) || "1".equals(text);
        return false;
    }

    private record VentaHeaderRow(
            Long id,
            Long clientId,
            String clientName,
            LocalDateTime soldAt,
            String notes,
            BigDecimal subtotalAmount,
            BigDecimal discountTotalAmount,
            BigDecimal globalDiscountPct,
            BigDecimal totalAmount,
            LocalDateTime createdAt
    ) {
    }

    private record LineCandidate(
            Long productId,
            Integer quantity,
            BigDecimal unitListPrice,
            BigDecimal lineDiscountPct,
            BigDecimal unitCost,
            Long bundleId
    ) {
    }

    private record BundleComponent(Long productId, Integer quantity) {
    }

    private record BundleDefinition(
            Long id,
            String name,
            BigDecimal discountPct,
            List<BundleComponent> components
    ) {
    }

    private static final class MutableBundleDefinition {
        private final Long id;
        private final String name;
        private final BigDecimal discountPct;
        private final boolean active;
        private final List<BundleComponent> components = new ArrayList<>();

        private MutableBundleDefinition(Long id, String name, BigDecimal discountPct, boolean active) {
            this.id = id;
            this.name = name;
            this.discountPct = discountPct;
            this.active = active;
        }
    }

    private static final class ProductSaleAccumulator {
        private final Long productId;
        private final BigDecimal unitListPrice;
        private final BigDecimal unitCost;
        private Long bundleId;
        private int quantity = 0;
        private BigDecimal listAmount = BigDecimal.ZERO;
        private BigDecimal netAmount = BigDecimal.ZERO;

        private ProductSaleAccumulator(Long productId, BigDecimal unitListPrice, BigDecimal unitCost, Long bundleId) {
            this.productId = productId;
            this.unitListPrice = unitListPrice;
            this.unitCost = unitCost;
            this.bundleId = bundleId;
        }
    }

    private record SaleItemInsert(
            Long productId,
            Long bundleId,
            Integer quantity,
            BigDecimal listUnitPrice,
            BigDecimal discountPct,
            BigDecimal netUnitPrice,
            BigDecimal unitCost,
            BigDecimal listAmount,
            BigDecimal netAmount
    ) {
    }
}
