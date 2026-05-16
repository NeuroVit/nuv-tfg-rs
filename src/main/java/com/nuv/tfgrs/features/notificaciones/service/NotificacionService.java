package com.nuv.tfgrs.features.notificaciones.service;

import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.notificaciones.dto.response.NotificacionResponse;

public interface NotificacionService {
    PagedResponse<NotificacionResponse> listarPaginado(Boolean soloNoLeidas, Integer limit, Integer offset);
    long contarNoLeidas();
    NotificacionResponse marcarLeida(Long id);
    int marcarTodasLeidas();
    void crearNotificacionAsignacionTarea(Long organizationId, Long recipientUserId, Long tareaId, String tituloTarea);
}
