package com.nuv.tfgrs.features.tareas.service.impl;

import com.nuv.tfgrs.features.tareas.dto.request.ActualizarEstadoTareaRequest;
import com.nuv.tfgrs.features.tareas.dto.request.CrearTareaRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResumenResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResponse;
import com.nuv.tfgrs.features.alertas.entity.AlertaStock;
import com.nuv.tfgrs.features.tareas.entity.Tarea;
import com.nuv.tfgrs.features.alertas.repository.AlertaStockRepository;
import com.nuv.tfgrs.features.tareas.repository.TareaRepository;
import com.nuv.tfgrs.features.tareas.service.TareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public PagedResponse<TareaResponse> listarPaginado(String estado, Integer limit, Integer offset) {
        sincronizarDesdeAlertas();
        int safeLimit = Math.max(1, Math.min(limit == null ? 9 : limit, 100));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int pageIndex = safeOffset / safeLimit;

        Page<Tarea> page = estado != null && !estado.isBlank()
                ? tareaRepository.findByEstadoOrderByCreatedAtDesc(estado, PageRequest.of(pageIndex, safeLimit))
                : tareaRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(pageIndex, safeLimit));

        List<TareaResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PagedResponse<>(content, page.getTotalElements(), safeLimit, safeOffset);
    }

    @Override
    @Transactional
    public TareaResumenResponse resumen() {
        sincronizarDesdeAlertas();
        long total = tareaRepository.count();
        long pendiente = tareaRepository.countByEstado("PENDIENTE");
        long enGestion = tareaRepository.countByEstado("EN_GESTION");
        long pospuesto = tareaRepository.countByEstado("POSPUESTO");
        long completado = tareaRepository.countByEstado("COMPLETADO");
        return new TareaResumenResponse(total, pendiente, enGestion, pospuesto, completado);
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
