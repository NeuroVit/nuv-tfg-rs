package com.nuv.tfgrs.features.productos.controller;

import com.nuv.tfgrs.features.inventario.dto.request.ActualizarStockRequest;
import com.nuv.tfgrs.features.productos.dto.request.CrearProductoRequest;
import com.nuv.tfgrs.features.productos.dto.request.EditarProductoRequest;
import com.nuv.tfgrs.features.bundles.dto.response.BundleUnlinkResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.productos.dto.response.ProductoResponse;
import com.nuv.tfgrs.features.productos.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listarTodos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

    @GetMapping("/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<ProductoResponse>> listarPaginado(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "providerId", required = false) Long providerId,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(productoService.listarPaginado(search, categoriaId, providerId, estado, limit, offset));
    }

    @GetMapping("/bajo-stock")
    public ResponseEntity<List<ProductoResponse>> listarBajoStock() {
        return ResponseEntity.ok(productoService.listarBajoStock());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<ProductoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody CrearProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EditarProductoRequest request) {
        return ResponseEntity.ok(productoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ProductoResponse> actualizarStock(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarStockRequest request) {
        return ResponseEntity.ok(productoService.actualizarStock(id, request));
    }

    @GetMapping("/{id}/bundles")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<String>> listarBundlesVinculados(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.listarBundlesVinculados(id));
    }

    @DeleteMapping("/{id}/bundles")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<BundleUnlinkResponse> desvincularDeBundles(@PathVariable Long id) {
        List<String> bundles = productoService.listarBundlesVinculados(id);
        int removed = productoService.desvincularDeBundles(id);
        return ResponseEntity.ok(new BundleUnlinkResponse(id, removed, bundles));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
