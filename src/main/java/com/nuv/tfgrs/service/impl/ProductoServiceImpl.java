package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.ActualizarStockRequest;
import com.nuv.tfgrs.dto.request.CrearProductoRequest;
import com.nuv.tfgrs.dto.request.EditarProductoRequest;
import com.nuv.tfgrs.dto.response.ProductoResponse;
import com.nuv.tfgrs.entity.Categoria;
import com.nuv.tfgrs.entity.MovimientoStock;
import com.nuv.tfgrs.entity.Producto;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.CategoriaRepository;
import com.nuv.tfgrs.repository.MovimientoStockRepository;
import com.nuv.tfgrs.repository.ProductoRepository;
import com.nuv.tfgrs.service.ProductoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MovimientoStockRepository movimientoStockRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll()
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarBajoStock() {
        return productoRepository.findLowStockProducts()
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        return ProductoResponse.from(findOrThrow(id));
    }

    @Override
    @Transactional
    public ProductoResponse crear(CrearProductoRequest request) {
        validarSkuDisponibleParaCrear(request.sku());
        Categoria categoria = findCategoriaOrThrowBadRequest(request.categoriaId());

        var producto = Producto.builder()
                .name(request.nombre())
                .sku(request.sku())
                .description(request.descripcion())
                .currentQuantity(request.cantidadActual())
                .minimumStockLevel(request.nivelMinimoStock())
                .unitPrice(request.precioUnitario())
                .category(categoria)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, EditarProductoRequest request) {
        var producto = findOrThrow(id);

        if (request.nombre() != null) {
            if (request.nombre().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no puede estar vacío");
            }
            producto.setName(request.nombre());
        }

        if (request.sku() != null) {
            if (request.sku().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El SKU no puede estar vacío");
            }
            validarSkuDisponibleParaActualizar(id, request.sku());
            producto.setSku(request.sku());
        }

        if (request.descripcion() != null) {
            producto.setDescription(request.descripcion());
        }
        if (request.cantidadActual() != null) {
            producto.setCurrentQuantity(request.cantidadActual());
        }
        if (request.nivelMinimoStock() != null) {
            producto.setMinimumStockLevel(request.nivelMinimoStock());
        }
        if (request.precioUnitario() != null) {
            producto.setUnitPrice(request.precioUnitario());
        }
        if (request.categoriaId() != null) {
            producto.setCategory(findCategoriaOrThrowBadRequest(request.categoriaId()));
        }
        producto.setUpdatedAt(LocalDateTime.now());

        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizarStock(Long id, ActualizarStockRequest request) {
        var producto = findOrThrow(id);
        int nuevaCantidad = producto.getCurrentQuantity() + request.delta();

        if (nuevaCantidad < 0) {
            throw new IllegalArgumentException(
                "Stock insuficiente. Actual: %d, Delta: %d".formatted(producto.getCurrentQuantity(), request.delta())
            );
        }

        try {
            producto.setCurrentQuantity(nuevaCantidad);
            producto.setUpdatedAt(LocalDateTime.now());
            Producto saved = productoRepository.save(producto);

            var movimiento = MovimientoStock.builder()
                    .product(saved)
                    .delta(request.delta())
                    .reason(request.razon())
                    .notes(request.notas())
                    .movedAt(LocalDateTime.now())
                    .build();
            movimientoStockRepository.save(movimiento);

            log.info("Stock actualizado correctamente");
            log.debug("Stock actualizado — producto={}, delta={}, razon={}, nuevo={}", id, request.delta(), request.razon(), nuevaCantidad);
            return ProductoResponse.from(saved);

        } catch (OptimisticLockingFailureException ex) {
            log.warn("Conflicto de concurrencia actualizando stock");
            log.debug("Conflicto de concurrencia actualizando stock del producto {}", id, ex);
            throw new OptimisticLockingFailureException(
                "El producto fue modificado por otra operación. Por favor reintenta.", ex
            );
        }
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = findOrThrow(id);
        try {
            productoRepository.delete(producto);
            productoRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            String detail = resolveDeleteConstraintMessage(id, ex);
            log.warn("No se pudo eliminar el producto {} por restricción de integridad", id);
            log.debug("Detalle restricción al eliminar producto {}", id, ex);
            throw new ResponseStatusException(HttpStatus.CONFLICT, detail, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listarBundlesVinculados(Long id) {
        findOrThrow(id);
        return findBundleNamesByProductId(id);
    }

    @Override
    @Transactional
    public int desvincularDeBundles(Long id) {
        findOrThrow(id);
        List<String> bundles = findBundleNamesByProductId(id);
        if (bundles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto no está asociado a ningún bundle");
        }

        String sql = "DELETE FROM bundle_items WHERE product_id = :productId";
        MapSqlParameterSource params = new MapSqlParameterSource("productId", id);
        int removed = jdbcTemplate.update(sql, params);

        log.info("Producto desvinculado de bundles");
        log.debug("Producto {} desvinculado de {} relación(es) en bundle_items", id, removed);
        return removed;
    }

    private Producto findOrThrow(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private Categoria findCategoriaOrThrowBadRequest(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoría no encontrada"));
    }

    private void validarSkuDisponibleParaCrear(String sku) {
        if (productoRepository.existsBySku(sku)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El SKU ya está en uso");
        }
    }

    private void validarSkuDisponibleParaActualizar(Long idProducto, String sku) {
        if (productoRepository.existsBySkuAndIdNot(sku, idProducto)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El SKU ya está en uso");
        }
    }

    private String resolveDeleteConstraintMessage(Long productId, DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();

        if (message == null) {
            return "No se puede eliminar el producto porque está referenciado por otros registros";
        }

        if (message.contains("fk_bundle_items_product")) {
            List<String> bundles = findBundleNamesByProductId(productId);
            if (bundles.isEmpty()) {
                return "No se puede eliminar el producto porque está asociado a uno o más bundles";
            }
            String detail = String.join(", ", bundles);
            return "No se puede eliminar el producto porque está asociado a bundles: " + detail;
        }

        return "No se puede eliminar el producto porque está referenciado por otros registros";
    }

    private List<String> findBundleNamesByProductId(Long productId) {
        String sql = """
                SELECT b.name
                FROM bundles b
                JOIN bundle_items bi ON bi.bundle_id = b.id
                WHERE bi.product_id = :productId
                ORDER BY b.name
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("productId", productId);
        return jdbcTemplate.queryForList(sql, params, String.class);
    }
}
