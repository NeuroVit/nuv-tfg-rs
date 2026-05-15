package com.nuv.tfgrs.features.tareas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ActualizarEstadoTareaRequest {

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "PENDIENTE|EN_GESTION|POSPUESTO|COMPLETADO",
             message = "Estado debe ser PENDIENTE, EN_GESTION, POSPUESTO o COMPLETADO")
    private String estado;
}
