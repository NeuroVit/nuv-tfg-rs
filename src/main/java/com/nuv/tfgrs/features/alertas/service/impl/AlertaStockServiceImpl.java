package com.nuv.tfgrs.features.alertas.service.impl;

import com.nuv.tfgrs.features.alertas.dto.response.AlertaResumenResponse;
import com.nuv.tfgrs.features.alertas.dto.response.AlertaStockResponse;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.alertas.entity.AlertaStock;
import com.nuv.tfgrs.features.productos.entity.Producto;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.features.alertas.repository.AlertaStockRepository;
import com.nuv.tfgrs.features.productos.repository.ProductoRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.features.alertas.service.AlertaStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AlertaStockServiceImpl implements AlertaStockService {

    private final AlertaStockRepository alertaStockRepository;
    private final ProductoRepository productoRepository;
    private final CurrentOrganizationResolver currentOrganizationResolver;
    private final StockAlertManager stockAlertManager;

    @Override
    @Transactional
    public List<AlertaStockResponse> listar(Boolean resueltas) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        syncInventoryAlertsForOrganization(organizationId);
        syncPriceAnomalyAlertsForOrganization(organizationId);
        normalizeOpenAlertDuplicates(organizationId);

        List<AlertaStock> alerts;
        if (resueltas == null) {
            alerts = alertaStockRepository.findByOrganizationId(organizationId);
        } else if (resueltas) {
            alerts = alertaStockRepository.findByOrganizationIdAndResolvedTrueOrderByCreatedAtDesc(organizationId);
        } else {
            alerts = alertaStockRepository.findByOrganizationIdAndResolvedFalseOrderByCreatedAtDesc(organizationId);
        }

        return alerts.stream()
                .map(AlertaStockResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PagedResponse<AlertaStockResponse> listarPaginado(Boolean resueltas, String search, Integer limit, Integer offset) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        syncInventoryAlertsForOrganization(organizationId);
        syncPriceAnomalyAlertsForOrganization(organizationId);
        normalizeOpenAlertDuplicates(organizationId);

        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int pageIndex = safeOffset / safeLimit;
        String normalizedSearch = normalizeLikeSearch(search);

        Page<AlertaStock> page = alertaStockRepository.findPagedByFilters(
                organizationId,
                resueltas,
                normalizedSearch,
                PageRequest.of(pageIndex, safeLimit)
        );

        List<AlertaStockResponse> content = page.getContent().stream().map(AlertaStockResponse::from).toList();
        return new PagedResponse<>(content, page.getTotalElements(), safeLimit, safeOffset);
    }

    @Override
    @Transactional
    public AlertaResumenResponse resumen() {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        syncInventoryAlertsForOrganization(organizationId);
        syncPriceAnomalyAlertsForOrganization(organizationId);
        normalizeOpenAlertDuplicates(organizationId);

        long abiertas = alertaStockRepository.countByOrganizationIdAndResolvedFalse(organizationId);
        long stockBajo = alertaStockRepository.countDistinctOpenProductsByType(
                organizationId,
                AlertaStock.AlertType.LOW_STOCK
        );
        long criticas = alertaStockRepository.countDistinctOpenProductsByType(
                organizationId,
                AlertaStock.AlertType.OUT_OF_STOCK
        );
        long anomaliasPrecio = alertaStockRepository.countDistinctOpenProductsByType(
                organizationId,
                AlertaStock.AlertType.PRICE_ANOMALY
        );
        long anomalias = alertaStockRepository.countDistinctOpenProductsByTypes(
                organizationId,
                EnumSet.of(AlertaStock.AlertType.PRICE_ANOMALY, AlertaStock.AlertType.OVERSTOCK)
        );
        long resueltasUltimos7Dias = alertaStockRepository.countByOrganizationIdAndResolvedTrueAndResolvedAtGreaterThanEqual(
                organizationId,
                LocalDateTime.now().minusDays(7)
        );

        return new AlertaResumenResponse(abiertas, stockBajo, anomaliasPrecio, resueltasUltimos7Dias, criticas, anomalias);
    }

    @Override
    @Transactional
    public AlertaStockResponse resolver(Long alertId) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        alertaStockRepository.findByIdAndOrganizationId(alertId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada: " + alertId));

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Las alertas se resuelven automáticamente al corregir el estado del producto: reponer stock, ajustar precios o actualizar límites."
        );
    }

    private void syncInventoryAlertsForOrganization(Long organizationId) {
        List<Producto> products = productoRepository.findByOrganizationId(organizationId);
        for (Producto product : products) {
            stockAlertManager.syncForProduct(product);
        }
    }

    private void syncPriceAnomalyAlertsForOrganization(Long organizationId) {
        List<Producto> anomalyProducts = productoRepository.findPriceAnomalyProductsByOrganizationId(organizationId);
        Set<Long> anomalyProductIds = new HashSet<>();
        for (Producto product : anomalyProducts) {
            anomalyProductIds.add(product.getId());
        }

        List<AlertaStock> openPriceAnomalyAlerts = alertaStockRepository
                .findByOrganizationIdAndAlertTypeAndResolvedFalseOrderByCreatedAtDesc(
                        organizationId,
                        AlertaStock.AlertType.PRICE_ANOMALY
                );
        Set<Long> alertedProductIds = new HashSet<>();
        for (AlertaStock alert : openPriceAnomalyAlerts) {
            alertedProductIds.add(alert.getProduct().getId());
        }

        for (Producto product : anomalyProducts) {
            if (alertedProductIds.contains(product.getId())) continue;
            alertaStockRepository.save(AlertaStock.builder()
                    .organizationId(organizationId)
                    .product(product)
                    .alertType(AlertaStock.AlertType.PRICE_ANOMALY)
                    .message("Anomalía de precio: precio de venta por debajo del coste en " + product.getName() + " (" + product.getSku() + ")")
                    .build());
        }

        LocalDateTime now = LocalDateTime.now();
        for (AlertaStock alert : openPriceAnomalyAlerts) {
            if (anomalyProductIds.contains(alert.getProduct().getId())) continue;
            alert.setResolved(true);
            alert.setResolvedAt(now);
        }
        alertaStockRepository.saveAll(openPriceAnomalyAlerts);
    }

    private String normalizeLikeSearch(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return null;
        return "%" + normalized + "%";
    }

    private void normalizeOpenAlertDuplicates(Long organizationId) {
        List<AlertaStock> openAlerts = alertaStockRepository.findByOrganizationIdAndResolvedFalseOrderByCreatedAtDesc(organizationId);
        if (openAlerts.isEmpty()) return;

        Map<String, AlertaStock> seenByProductAndType = new HashMap<>();
        List<AlertaStock> duplicatesToResolve = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AlertaStock alert : openAlerts) {
            String key = alert.getProduct().getId() + "::" + alert.getAlertType().name();
            if (!seenByProductAndType.containsKey(key)) {
                seenByProductAndType.put(key, alert);
                continue;
            }
            alert.setResolved(true);
            alert.setResolvedAt(now);
            duplicatesToResolve.add(alert);
        }

        if (!duplicatesToResolve.isEmpty()) {
            alertaStockRepository.saveAll(duplicatesToResolve);
        }
    }
}
