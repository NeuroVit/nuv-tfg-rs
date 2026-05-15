package com.nuv.tfgrs.features.bundles.controller;

import com.nuv.tfgrs.features.bundles.dto.request.BundleUpsertRequest;
import com.nuv.tfgrs.features.bundles.dto.response.BundleItemResponse;
import com.nuv.tfgrs.features.bundles.dto.response.BundleSummaryResponse;
import com.nuv.tfgrs.features.bundles.service.BundleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bundles")
@RequiredArgsConstructor
public class BundleController {

    private final BundleService bundleService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BundleSummaryResponse>> listarBundles() {
        return ResponseEntity.ok(bundleService.listarBundles());
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BundleItemResponse>> listarItems(@PathVariable Long id) {
        return ResponseEntity.ok(bundleService.listarItemsDeBundle(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<BundleSummaryResponse> crear(@Valid @RequestBody BundleUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bundleService.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<BundleSummaryResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody BundleUpsertRequest request
    ) {
        return ResponseEntity.ok(bundleService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        bundleService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
