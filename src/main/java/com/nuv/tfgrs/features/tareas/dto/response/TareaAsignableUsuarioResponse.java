package com.nuv.tfgrs.features.tareas.dto.response;

public record TareaAsignableUsuarioResponse(
        Long id,
        String username,
        String nombreCompleto,
        String role
) {
}
