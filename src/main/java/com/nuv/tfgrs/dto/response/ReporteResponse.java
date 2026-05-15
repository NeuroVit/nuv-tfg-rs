package com.nuv.tfgrs.dto.response;

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
    private String generadoPor;
    private LocalDateTime createdAt;
}
