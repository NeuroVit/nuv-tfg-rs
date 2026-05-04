package com.nuv.tfgrs.dto.response;

import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        String notas,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
