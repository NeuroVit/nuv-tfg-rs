package com.nuv.tfgrs.features.tareas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ActualizarAsignadoTareaRequest {

    @NotNull(message = "El usuario asignado es obligatorio")
    @Positive(message = "El usuario asignado debe ser válido")
    private Long asignadoAUserId;
}
