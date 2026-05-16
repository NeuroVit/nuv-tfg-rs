package com.nuv.tfgrs.features.notificaciones.dto.response;

import com.nuv.tfgrs.features.notificaciones.entity.Notificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        String tipo,
        String titulo,
        String descripcion,
        String targetPath,
        Long tareaId,
        boolean leida,
        LocalDateTime creadaEn,
        LocalDateTime leidaEn
) {
    public static NotificacionResponse from(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getTitulo(),
                notificacion.getDescripcion(),
                notificacion.getTargetPath(),
                notificacion.getTareaId(),
                Boolean.TRUE.equals(notificacion.getLeida()),
                notificacion.getCreatedAt(),
                notificacion.getReadAt()
        );
    }
}
