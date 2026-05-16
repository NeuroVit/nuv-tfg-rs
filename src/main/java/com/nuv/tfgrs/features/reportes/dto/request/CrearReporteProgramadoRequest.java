package com.nuv.tfgrs.features.reportes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CrearReporteProgramadoRequest {

    @NotBlank(message = "El tipo es obligatorio")
    @Pattern(regexp = "INVENTARIO|ALERTAS|CLIENTES|MOVIMIENTOS|VENTAS",
             message = "Tipo debe ser INVENTARIO, ALERTAS, CLIENTES, MOVIMIENTOS o VENTAS")
    private String tipo;

    @NotBlank(message = "La frecuencia es obligatoria")
    @Pattern(regexp = "DIARIO|SEMANAL|MENSUAL",
             message = "Frecuencia debe ser DIARIO, SEMANAL o MENSUAL")
    private String frecuencia;
}
