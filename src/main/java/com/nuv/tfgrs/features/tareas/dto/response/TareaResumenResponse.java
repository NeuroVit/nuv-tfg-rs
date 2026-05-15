package com.nuv.tfgrs.features.tareas.dto.response;

public record TareaResumenResponse(
        long total,
        long pendiente,
        long enGestion,
        long pospuesto,
        long completado
) {
}
