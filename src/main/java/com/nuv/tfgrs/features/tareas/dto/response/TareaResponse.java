package com.nuv.tfgrs.features.tareas.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TareaResponse {
    private Long id;
    private String titulo;
    private String descripcion;
    private String tipo;
    private String estado;
    private String prioridad;
    private Long alertaId;
    private String productoNombre;
    private String sku;
    private String alertaTipo;
    private String creadoPor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
