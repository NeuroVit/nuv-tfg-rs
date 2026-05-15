package com.nuv.tfgrs.service.impl;

import com.nuv.tfgrs.dto.request.ActualizarEstadoTareaRequest;
import com.nuv.tfgrs.dto.request.CrearTareaRequest;
import com.nuv.tfgrs.dto.response.TareaResponse;
import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.entity.Tarea;
import com.nuv.tfgrs.repository.AlertaStockRepository;
import com.nuv.tfgrs.repository.TareaRepository;
import com.nuv.tfgrs.service.TareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TareaServiceImpl implements TareaService {

    private final TareaRepository tareaRepository;
    private final AlertaStockRepository alertaStockRepository;

    @Override
    @Transactional
    public List<TareaResponse> listar(String estado) {
        sincronizarDesdeAlertas();
        List<Tarea> tareas = estado != null
                ? tareaRepository.findByEstadoOrderByCreatedAtDesc(estado)
                : tareaRepository.findAllByOrderByCreatedAtDesc();
        return tareas.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TareaResponse crear(CrearTareaRequest request, String username) {
        Tarea tarea = Tarea.builder()
                .titulo(request.getTitulo().trim())
                .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
                .tipo("MANUAL")
                .estado("PENDIENTE")
                .prioridad(request.getPrioridad() != null ? request.getPrioridad() : "MEDIA")
                .creadoPor(username)
                .build();
        return toResponse(tareaRepository.save(tarea));
    }

    @Override
    @Transactional
    public TareaResponse actualizarEstado(Long id, ActualizarEstadoTareaRequest request) {
        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        tarea.setEstado(request.getEstado());
        return toResponse(tareaRepository.save(tarea));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!tareaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        tareaRepository.deleteById(id);
    }

    private void sincronizarDesdeAlertas() {
        List<AlertaStock> alertasPendientes =
                alertaStockRepository.findByResolvedFalseOrderByCreatedAtDesc();

        for (AlertaStock alerta : alertasPendientes) {
            if (tareaRepository.existsByAlertaId(alerta.getId())) continue;

            String productoNombre = alerta.getProduct() != null ? alerta.getProduct().getName() : null;
            String sku            = alerta.getProduct() != null ? alerta.getProduct().getSku()  : null;
            String alertaTipo     = alerta.getAlertType() != null ? alerta.getAlertType().name() : null;

            Tarea tarea = Tarea.builder()
                    .titulo(buildTitulo(alertaTipo, productoNombre))
                    .descripcion(alerta.getMessage())
                    .tipo("ALERTA_STOCK")
                    .estado("PENDIENTE")
                    .prioridad(mapPrioridad(alerta.getAlertType()))
                    .alertaId(alerta.getId())
                    .productoNombre(productoNombre)
                    .sku(sku)
                    .alertaTipo(alertaTipo)
                    .build();

            tareaRepository.save(tarea);
        }
    }

    private String buildTitulo(String alertaTipo, String productoNombre) {
        String producto = productoNombre != null ? productoNombre : "Producto desconocido";
        return switch (alertaTipo != null ? alertaTipo : "") {
            case "LOW_STOCK"     -> "Stock bajo: " + producto;
            case "OUT_OF_STOCK"  -> "Sin stock: " + producto;
            case "OVERSTOCK"     -> "Exceso de stock: " + producto;
            case "PRICE_ANOMALY" -> "Anomalía de precio: " + producto;
            default              -> "Alerta de stock: " + producto;
        };
    }

    private String mapPrioridad(AlertaStock.AlertType tipo) {
        if (tipo == null) return "MEDIA";
        return switch (tipo) {
            case OUT_OF_STOCK                -> "ALTA";
            case LOW_STOCK                   -> "MEDIA";
            case OVERSTOCK, PRICE_ANOMALY    -> "BAJA";
        };
    }

    private TareaResponse toResponse(Tarea t) {
        return TareaResponse.builder()
                .id(t.getId())
                .titulo(t.getTitulo())
                .descripcion(t.getDescripcion())
                .tipo(t.getTipo())
                .estado(t.getEstado())
                .prioridad(t.getPrioridad())
                .alertaId(t.getAlertaId())
                .productoNombre(t.getProductoNombre())
                .sku(t.getSku())
                .alertaTipo(t.getAlertaTipo())
                .creadoPor(t.getCreadoPor())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
