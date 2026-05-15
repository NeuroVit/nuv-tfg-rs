package com.nuv.tfgrs.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReporteProgramadoResponse {
    private Long id;
    private String tipo;
    private String frecuencia;
    private Boolean activo;
    private String creadoPor;
    private LocalDateTime ultimaEjecucion;
    private LocalDateTime proximaEjecucion;
    private LocalDateTime createdAt;
}
