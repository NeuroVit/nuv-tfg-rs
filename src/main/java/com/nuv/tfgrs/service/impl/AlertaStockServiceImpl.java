package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.response.AlertaStockResponse;
import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.repository.AlertaStockRepository;
import com.nuv.tfgrs.service.AlertaStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaStockServiceImpl implements AlertaStockService {

    private final AlertaStockRepository alertaStockRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AlertaStockResponse> listar(Boolean resueltas) {
        List<AlertaStock> alerts;
        if (resueltas == null) {
            alerts = alertaStockRepository.findAll();
        } else if (resueltas) {
            alerts = alertaStockRepository.findByResolvedTrueOrderByCreatedAtDesc();
        } else {
            alerts = alertaStockRepository.findByResolvedFalseOrderByCreatedAtDesc();
        }

        return alerts.stream()
                .map(AlertaStockResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AlertaStockResponse resolver(Long alertId) {
        AlertaStock alert = alertaStockRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada: " + alertId));

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        return AlertaStockResponse.from(alertaStockRepository.save(alert));
    }
}
