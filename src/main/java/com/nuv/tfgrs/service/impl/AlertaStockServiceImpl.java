package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.AlertaStockResponse;
import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.entity.Producto;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.AlertaStockRepository;
import com.nuv.tfgrs.repository.ProductoRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.service.AlertaStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertaStockServiceImpl implements AlertaStockService {

    private final AlertaStockRepository alertaStockRepository;
    private final ProductoRepository productoRepository;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional
    public List<AlertaStockResponse> listar(Boolean resueltas) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        syncPriceAnomalyAlertsForOrganization(organizationId);

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
    public AlertaStockResponse resolver(Long alertId) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        AlertaStock alert = alertaStockRepository.findByIdAndOrganizationId(alertId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada: " + alertId));

        if (isActiveStockAlert(alert) && productStillUnderStockThreshold(alert)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede resolver la alerta mientras el producto siga con stock bajo. Repon el inventario primero."
            );
        }

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return AlertaStockResponse.from(alertaStockRepository.save(alert));
    }

    private boolean isActiveStockAlert(AlertaStock alert) {
        return alert.getAlertType() == AlertaStock.AlertType.LOW_STOCK
                || alert.getAlertType() == AlertaStock.AlertType.OUT_OF_STOCK;
    }

    private boolean productStillUnderStockThreshold(AlertaStock alert) {
        Integer currentQuantity = alert.getProduct().getCurrentQuantity();
        Integer minimumStockLevel = alert.getProduct().getMinimumStockLevel();
        int current = currentQuantity == null ? 0 : currentQuantity;
        int minimum = minimumStockLevel == null ? 0 : minimumStockLevel;
        return current <= minimum;
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
}
