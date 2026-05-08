package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.entity.Producto;
import com.nuv.tfgrs.repository.AlertaStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StockAlertManager {

    private final AlertaStockRepository alertaStockRepository;

    public void syncForProduct(Producto producto) {
        if (producto == null || producto.getId() == null) return;

        int current = producto.getCurrentQuantity() == null ? 0 : producto.getCurrentQuantity();
        int minimum = producto.getMinimumStockLevel() == null ? 0 : producto.getMinimumStockLevel();

        if (current <= 0) {
            createIfMissing(
                    producto,
                    AlertaStock.AlertType.OUT_OF_STOCK,
                    "Sin stock: " + producto.getName() + " (" + producto.getSku() + ")"
            );
            return;
        }

        if (current <= minimum) {
            createIfMissing(
                    producto,
                    AlertaStock.AlertType.LOW_STOCK,
                    "Stock bajo: " + producto.getName() + " (" + producto.getSku() + ") - actual "
                            + current + ", minimo " + minimum
            );
            return;
        }

        resolveOpenStockAlerts(producto.getId());
    }

    private void createIfMissing(Producto producto, AlertaStock.AlertType type, String message) {
        if (alertaStockRepository.existsByProductIdAndAlertTypeAndResolvedFalse(producto.getId(), type)) {
            return;
        }
        AlertaStock alert = AlertaStock.builder()
                .product(producto)
                .alertType(type)
                .message(message)
                .build();
        alertaStockRepository.save(alert);
    }

    private void resolveOpenStockAlerts(Long productId) {
        List<AlertaStock> openAlerts = alertaStockRepository.findByProductIdAndResolvedFalse(productId);
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
}
