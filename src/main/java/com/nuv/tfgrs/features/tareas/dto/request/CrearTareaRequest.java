package com.nuv.tfgrs.features.tareas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CrearTareaRequest {

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String descripcion;

    @Pattern(regexp = "ALTA|MEDIA|BAJA", message = "Prioridad debe ser ALTA, MEDIA o BAJA")
    private String prioridad = "MEDIA";

    @Positive(message = "El usuario asignado debe ser válido")
    private Long asignadoAUserId;
}
