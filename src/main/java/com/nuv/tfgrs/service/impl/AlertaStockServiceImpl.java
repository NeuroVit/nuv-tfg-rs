package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.AlertaStockResponse;
import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.AlertaStockRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.service.AlertaStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaStockServiceImpl implements AlertaStockService {

    private final AlertaStockRepository alertaStockRepository;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional(readOnly = true)
    public List<AlertaStockResponse> listar(Boolean resueltas) {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
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
}
