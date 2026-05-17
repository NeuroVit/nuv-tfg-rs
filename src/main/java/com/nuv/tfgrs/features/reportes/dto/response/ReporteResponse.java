package com.nuv.tfgrs.features.reportes.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReporteResponse {
    private Long id;
    private String nombre;
    private String tipo;
    private String descripcion;
    private String datos;
    private String analisisIa;
    private String analisisIaEstado;
    private String analisisIaModelo;
    private LocalDateTime analisisIaGeneradoEn;
    private String analisisIaError;
    private String generadoPor;
    private LocalDateTime createdAt;
}
