package com.nuv.tfgrs.features.notificaciones.service.impl;

import com.nuv.tfgrs.exception.ResourceNotFoundException;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.notificaciones.dto.response.NotificacionResponse;
import com.nuv.tfgrs.features.notificaciones.entity.Notificacion;
import com.nuv.tfgrs.features.notificaciones.repository.NotificacionRepository;
import com.nuv.tfgrs.features.notificaciones.service.NotificacionService;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private static final String TIPO_TASK_ASSIGNED = "TASK_ASSIGNED";
    private static final String TARGET_TAREAS = "/tareas";

    private final NotificacionRepository notificacionRepository;
    private final CurrentOrganizationResolver currentOrganizationResolver;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificacionResponse> listarPaginado(Boolean soloNoLeidas, Integer limit, Integer offset) {
        var context = currentOrganizationResolver.requireMembership();
        int safeLimit = Math.max(1, Math.min(limit == null ? 20 : limit, 100));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int pageIndex = safeOffset / safeLimit;
        boolean filterOnlyUnread = Boolean.TRUE.equals(soloNoLeidas);

        Page<Notificacion> page = notificacionRepository.findVisibleByRecipient(
                context.organizationId(),
                context.currentUser().getId(),
                filterOnlyUnread,
                PageRequest.of(pageIndex, safeLimit)
        );
        List<NotificacionResponse> content = page.getContent().stream().map(NotificacionResponse::from).toList();
        return new PagedResponse<>(content, page.getTotalElements(), safeLimit, safeOffset);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNoLeidas() {
        var context = currentOrganizationResolver.requireMembership();
        return notificacionRepository.countByOrganizationIdAndRecipientUserIdAndLeidaFalse(
                context.organizationId(),
                context.currentUser().getId()
        );
    }

    @Override
    @Transactional
    public NotificacionResponse marcarLeida(Long id) {
        var context = currentOrganizationResolver.requireMembership();
        Notificacion notification = notificacionRepository
                .findByIdAndOrganizationIdAndRecipientUserId(id, context.organizationId(), context.currentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada: " + id));

        if (!Boolean.TRUE.equals(notification.getLeida())) {
            notification.setLeida(true);
            notification.setReadAt(LocalDateTime.now(ZoneOffset.UTC));
            notification = notificacionRepository.save(notification);
        }
        return NotificacionResponse.from(notification);
    }

    @Override
    @Transactional
    public int marcarTodasLeidas() {
        var context = currentOrganizationResolver.requireMembership();
        return notificacionRepository.markAllAsRead(
                context.organizationId(),
                context.currentUser().getId(),
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }

    @Override
    @Transactional
    public void crearNotificacionAsignacionTarea(Long organizationId, Long recipientUserId, Long tareaId, String tituloTarea) {
        Notificacion notification = Notificacion.builder()
                .organizationId(organizationId)
                .recipientUserId(recipientUserId)
                .tipo(TIPO_TASK_ASSIGNED)
                .titulo("Tarea asignada: " + tituloTarea)
                .descripcion("Tienes una tarea pendiente asignada")
                .targetPath(TARGET_TAREAS)
                .tareaId(tareaId)
                .leida(false)
                .build();
        notificacionRepository.save(notification);
    }
}
