package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CrearReporteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El tipo es obligatorio")
    @Pattern(regexp = "INVENTARIO|ALERTAS|CLIENTES|MOVIMIENTOS|VENTAS",
             message = "Tipo debe ser INVENTARIO, ALERTAS, CLIENTES, MOVIMIENTOS o VENTAS")
    private String tipo;

    private String descripcion;
}
