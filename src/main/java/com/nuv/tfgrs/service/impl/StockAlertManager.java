package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.entity.Producto;
import com.nuv.tfgrs.repository.AlertaStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StockAlertManager {

    private final AlertaStockRepository alertaStockRepository;

    public void syncForProduct(Producto producto) {
        if (producto == null || producto.getId() == null) return;
        if (producto.getOrganizationId() == null) return;

        Long organizationId = producto.getOrganizationId();
        syncStockAlerts(organizationId, producto);
        syncPriceAnomalyAlerts(organizationId, producto);
    }

    private void syncStockAlerts(Long organizationId, Producto producto) {
        int current = producto.getCurrentQuantity() == null ? 0 : producto.getCurrentQuantity();
        int minimum = producto.getMinimumStockLevel() == null ? 0 : producto.getMinimumStockLevel();

        if (current <= 0) {
            createIfMissing(
                    organizationId,
                    producto,
                    AlertaStock.AlertType.OUT_OF_STOCK,
                    "Sin stock: " + producto.getName() + " (" + producto.getSku() + ")"
            );
            return;
        }

        if (current <= minimum) {
            createIfMissing(
                    organizationId,
                    producto,
                    AlertaStock.AlertType.LOW_STOCK,
                    "Stock bajo: " + producto.getName() + " (" + producto.getSku() + ") - actual "
                            + current + ", minimo " + minimum
            );
            return;
        }

        resolveOpenStockAlerts(organizationId, producto.getId());
    }

    private void syncPriceAnomalyAlerts(Long organizationId, Producto producto) {
        BigDecimal unitPrice = producto.getUnitPrice();
        BigDecimal acquisitionPrice = producto.getAcquisitionPrice();
        boolean hasPriceAnomaly = unitPrice != null
                && acquisitionPrice != null
                && unitPrice.compareTo(acquisitionPrice) < 0;

        if (hasPriceAnomaly) {
            createIfMissing(
                    organizationId,
                    producto,
                    AlertaStock.AlertType.PRICE_ANOMALY,
                    "Anomalía de precio: precio de venta "
                            + formatMoney(unitPrice)
                            + " por debajo del coste "
                            + formatMoney(acquisitionPrice)
                            + " en "
                            + producto.getName()
                            + " ("
                            + producto.getSku()
                            + ")"
            );
            return;
        }

        resolveOpenPriceAnomalyAlerts(organizationId, producto.getId());
    }

    private void createIfMissing(Long organizationId, Producto producto, AlertaStock.AlertType type, String message) {
        if (alertaStockRepository.existsByOrganizationIdAndProductIdAndAlertTypeAndResolvedFalse(
                organizationId,
                producto.getId(),
                type
        )) {
            return;
        }
        AlertaStock alert = AlertaStock.builder()
                .organizationId(organizationId)
                .product(producto)
                .alertType(type)
                .message(message)
                .build();
        alertaStockRepository.save(alert);
    }

    private void resolveOpenStockAlerts(Long organizationId, Long productId) {
        List<AlertaStock> openAlerts =
                alertaStockRepository.findByOrganizationIdAndProductIdAndResolvedFalse(organizationId, productId);
        if (openAlerts.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (AlertaStock alert : openAlerts) {
            if (alert.getAlertType() == AlertaStock.AlertType.LOW_STOCK
                    || alert.getAlertType() == AlertaStock.AlertType.OUT_OF_STOCK) {
                alert.setResolved(true);
                alert.setResolvedAt(now);
            }
        }
        alertaStockRepository.saveAll(openAlerts);
    }

    private void resolveOpenPriceAnomalyAlerts(Long organizationId, Long productId) {
        List<AlertaStock> openAlerts =
                alertaStockRepository.findByOrganizationIdAndProductIdAndResolvedFalse(organizationId, productId);
        if (openAlerts.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        for (AlertaStock alert : openAlerts) {
            if (alert.getAlertType() == AlertaStock.AlertType.PRICE_ANOMALY) {
                alert.setResolved(true);
                alert.setResolvedAt(now);
            }
        }
        alertaStockRepository.saveAll(openAlerts);
    }

    private String formatMoney(BigDecimal value) {
        return "€" + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
