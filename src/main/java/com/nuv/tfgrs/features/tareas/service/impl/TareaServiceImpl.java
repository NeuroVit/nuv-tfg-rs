package com.nuv.tfgrs.features.tareas.service.impl;

import com.nuv.tfgrs.features.tareas.dto.request.ActualizarEstadoTareaRequest;
import com.nuv.tfgrs.features.tareas.dto.request.ActualizarAsignadoTareaRequest;
import com.nuv.tfgrs.features.tareas.dto.request.CrearTareaRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaAsignableUsuarioResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResumenResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResponse;
import com.nuv.tfgrs.features.alertas.entity.AlertaStock;
import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership;
import com.nuv.tfgrs.features.organizaciones.repository.OrganizationMembershipRepository;
import com.nuv.tfgrs.features.tareas.entity.Tarea;
import com.nuv.tfgrs.features.notificaciones.service.NotificacionService;
import com.nuv.tfgrs.features.usuarios.entity.Usuario;
import com.nuv.tfgrs.features.usuarios.repository.UsuarioRepository;
import com.nuv.tfgrs.features.alertas.repository.AlertaStockRepository;
import com.nuv.tfgrs.security.org.CurrentOrganizationResolver;
import com.nuv.tfgrs.features.tareas.repository.TareaRepository;
import com.nuv.tfgrs.features.tareas.service.TareaService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TareaServiceImpl implements TareaService {

    private final TareaRepository tareaRepository;
    private final AlertaStockRepository alertaStockRepository;
    private final CurrentOrganizationResolver currentOrganizationResolver;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional
    public List<TareaResponse> listar(String estado) {
        var context = currentOrganizationResolver.requireMembership();
        sincronizarDesdeAlertas(context.organizationId());
        String safeEstado = normalizeEstado(estado);
        boolean includeAlertTasks = canSeeAlertTasks(context.currentUser());
        List<Tarea> tareas = tareaRepository.findVisibleByOrganizationAndEstado(
                context.organizationId(),
                context.currentUser().getId(),
                safeEstado,
                includeAlertTasks
        );
        return tareas.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PagedResponse<TareaResponse> listarPaginado(String estado, Integer limit, Integer offset) {
        var context = currentOrganizationResolver.requireMembership();
        sincronizarDesdeAlertas(context.organizationId());
        int safeLimit = Math.max(1, Math.min(limit == null ? 9 : limit, 100));
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        int pageIndex = safeOffset / safeLimit;
        String safeEstado = normalizeEstado(estado);
        boolean includeAlertTasks = canSeeAlertTasks(context.currentUser());

        Page<Tarea> page = tareaRepository.findVisibleByOrganizationAndEstado(
                context.organizationId(),
                context.currentUser().getId(),
                safeEstado,
                includeAlertTasks,
                PageRequest.of(pageIndex, safeLimit)
        );

        List<TareaResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return new PagedResponse<>(content, page.getTotalElements(), safeLimit, safeOffset);
    }

    @Override
    @Transactional
    public TareaResumenResponse resumen() {
        var context = currentOrganizationResolver.requireMembership();
        Long organizationId = context.organizationId();
        Long currentUserId = context.currentUser().getId();
        boolean includeAlertTasks = canSeeAlertTasks(context.currentUser());

        sincronizarDesdeAlertas(organizationId);
        long total = tareaRepository.countVisible(organizationId, currentUserId, includeAlertTasks);
        long pendiente = tareaRepository.countVisibleByEstado(organizationId, currentUserId, "PENDIENTE", includeAlertTasks);
        long enGestion = tareaRepository.countVisibleByEstado(organizationId, currentUserId, "EN_GESTION", includeAlertTasks);
        long pospuesto = tareaRepository.countVisibleByEstado(organizationId, currentUserId, "POSPUESTO", includeAlertTasks);
        long completado = tareaRepository.countVisibleByEstado(organizationId, currentUserId, "COMPLETADO", includeAlertTasks);
        return new TareaResumenResponse(total, pendiente, enGestion, pospuesto, completado);
    }

    @Override
    @Transactional
    public List<TareaAsignableUsuarioResponse> listarAsignables() {
        Long organizationId = currentOrganizationResolver.requireMembership().organizationId();
        return organizationMembershipRepository.findByOrganizationIdOrderByUserUsernameAsc(organizationId).stream()
                .map(OrganizationMembership::getUser)
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .map(u -> new TareaAsignableUsuarioResponse(
                        u.getId(),
                        u.getUsername(),
                        buildFullName(u),
                        u.getRole().name()
                ))
                .toList();
    }

    @Override
    @Transactional
    public TareaResponse crear(CrearTareaRequest request) {
        var context = currentOrganizationResolver.requireMembership();
        Long organizationId = context.organizationId();
        Usuario creator = context.currentUser();
        Usuario assignee = resolveAssigneeForManualTask(
                organizationId,
                request.getAsignadoAUserId(),
                creator
        );

        Tarea tarea = Tarea.builder()
                .organizationId(organizationId)
                .titulo(request.getTitulo().trim())
                .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
                .tipo("MANUAL")
                .estado("PENDIENTE")
                .prioridad(request.getPrioridad() != null ? request.getPrioridad() : "MEDIA")
                .creadoPor(creator.getUsername())
                .creadoPorUserId(creator.getId())
                .asignadoAUserId(assignee.getId())
                .build();
        Tarea saved = tareaRepository.save(tarea);
        notificacionService.crearNotificacionAsignacionTarea(
                organizationId,
                assignee.getId(),
                saved.getId(),
                saved.getTitulo()
        );
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TareaResponse actualizarEstado(Long id, ActualizarEstadoTareaRequest request) {
        var context = currentOrganizationResolver.requireMembership();
        Tarea tarea = findVisibleTaskOrThrow(id, context.organizationId(), context.currentUser().getId());
        tarea.setEstado(request.getEstado());
        return toResponse(tareaRepository.save(tarea));
    }

    @Override
    @Transactional
    public TareaResponse actualizarAsignado(Long id, ActualizarAsignadoTareaRequest request) {
        var context = currentOrganizationResolver.requireMembership();
        Usuario actor = context.currentUser();
        Tarea tarea = findVisibleTaskOrThrow(id, context.organizationId(), actor.getId());
        if (!canReassign(actor, tarea)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para reasignar esta tarea");
        }

        Usuario assignee = resolveAssigneeForManualTask(context.organizationId(), request.getAsignadoAUserId(), actor);
        if ("ALERTA_STOCK".equals(tarea.getTipo()) && !canSeeAlertTasks(assignee)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las tareas de alerta de stock solo se pueden asignar a usuarios con rol Manager o Admin"
            );
        }
        if (tarea.getAsignadoAUserId().equals(assignee.getId())) {
            return toResponse(tarea);
        }
        tarea.setAsignadoAUserId(assignee.getId());
        Tarea saved = tareaRepository.save(tarea);
        notificacionService.crearNotificacionAsignacionTarea(
                context.organizationId(),
                assignee.getId(),
                saved.getId(),
                saved.getTitulo()
        );
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        var context = currentOrganizationResolver.requireMembership();
        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        if (!tarea.getOrganizationId().equals(context.organizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        tareaRepository.deleteById(id);
    }

    private void sincronizarDesdeAlertas(Long organizationId) {
        marcarTareasCompletadasPorAlertaResuelta(organizationId);

        List<OrganizationMembership> responsables = organizationMembershipRepository
                .findActiveByOrganizationIdAndUserRoleIn(
                        organizationId,
                        List.of(Usuario.Role.ROLE_ADMIN, Usuario.Role.ROLE_MANAGER)
                );
        if (responsables.isEmpty()) {
            responsables = organizationMembershipRepository.findByOrganizationIdOrderByUserUsernameAsc(organizationId)
                    .stream()
                    .filter(om -> Boolean.TRUE.equals(om.getUser().getActive()))
                    .toList();
        }
        if (responsables.isEmpty()) return;

        List<AlertaStock> alertasPendientes =
                alertaStockRepository.findByOrganizationIdAndResolvedFalseOrderByCreatedAtDesc(organizationId);

        for (AlertaStock alerta : alertasPendientes) {
            String productoNombre = alerta.getProduct() != null ? alerta.getProduct().getName() : null;
            String sku            = alerta.getProduct() != null ? alerta.getProduct().getSku()  : null;
            String alertaTipo     = alerta.getAlertType() != null ? alerta.getAlertType().name() : null;
            for (OrganizationMembership responsable : responsables) {
                Usuario usuarioAsignado = responsable.getUser();
                if (tareaRepository.existsByOrganizationIdAndAlertaIdAndAsignadoAUserId(
                        organizationId,
                        alerta.getId(),
                        usuarioAsignado.getId()
                )) {
                    continue;
                }

                Tarea tarea = Tarea.builder()
                        .organizationId(organizationId)
                        .titulo(buildTitulo(alertaTipo, productoNombre))
                        .descripcion(alerta.getMessage())
                        .tipo("ALERTA_STOCK")
                        .estado("PENDIENTE")
                        .prioridad(mapPrioridad(alerta.getAlertType()))
                        .alertaId(alerta.getId())
                        .productoNombre(productoNombre)
                        .sku(sku)
                        .alertaTipo(alertaTipo)
                        .creadoPor(usuarioAsignado.getUsername())
                        .creadoPorUserId(usuarioAsignado.getId())
                        .asignadoAUserId(usuarioAsignado.getId())
                        .build();

                try {
                    Tarea saved = tareaRepository.save(tarea);
                    notificacionService.crearNotificacionAsignacionTarea(
                            organizationId,
                            usuarioAsignado.getId(),
                            saved.getId(),
                            saved.getTitulo()
                    );
                } catch (DataIntegrityViolationException ex) {
                    if (!isDuplicateAlertAssignmentConstraint(ex)) {
                        throw ex;
                    }
                }
            }
        }
    }

    private void marcarTareasCompletadasPorAlertaResuelta(Long organizationId) {
        List<Tarea> tareasAbiertasAlerta = tareaRepository.findByOrganizationIdAndTipoAndAlertaIdIsNotNullAndEstadoNot(
                organizationId,
                "ALERTA_STOCK",
                "COMPLETADO"
        );
        if (tareasAbiertasAlerta.isEmpty()) return;

        Set<Long> alertasResueltas = alertaStockRepository
                .findByOrganizationIdAndResolvedTrueOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(AlertaStock::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (alertasResueltas.isEmpty()) return;

        boolean changed = false;
        for (Tarea tarea : tareasAbiertasAlerta) {
            if (tarea.getAlertaId() == null) continue;
            if (!alertasResueltas.contains(tarea.getAlertaId())) continue;
            tarea.setEstado("COMPLETADO");
            changed = true;
        }
        if (changed) {
            tareaRepository.saveAll(tareasAbiertasAlerta);
        }
    }

    private String buildTitulo(String alertaTipo, String productoNombre) {
        String producto = productoNombre != null ? productoNombre : "Producto desconocido";
        return switch (alertaTipo != null ? alertaTipo : "") {
            case "LOW_STOCK"     -> "Stock bajo: " + producto;
            case "OUT_OF_STOCK"  -> "Sin stock: " + producto;
            case "OVERSTOCK"     -> "Exceso de stock: " + producto;
            case "PRICE_ANOMALY" -> "Anomalía de precio: " + producto;
            default              -> "Alerta de stock: " + producto;
        };
    }

    private String mapPrioridad(AlertaStock.AlertType tipo) {
        if (tipo == null) return "MEDIA";
        return switch (tipo) {
            case OUT_OF_STOCK                -> "ALTA";
            case LOW_STOCK                   -> "MEDIA";
            case OVERSTOCK, PRICE_ANOMALY    -> "BAJA";
        };
    }

    private TareaResponse toResponse(Tarea t) {
        Usuario asignado = usuarioRepository.findById(t.getAsignadoAUserId()).orElse(null);
        return TareaResponse.builder()
                .id(t.getId())
                .organizationId(t.getOrganizationId())
                .titulo(t.getTitulo())
                .descripcion(t.getDescripcion())
                .tipo(t.getTipo())
                .estado(t.getEstado())
                .prioridad(t.getPrioridad())
                .alertaId(t.getAlertaId())
                .productoNombre(t.getProductoNombre())
                .sku(t.getSku())
                .alertaTipo(t.getAlertaTipo())
                .creadoPor(t.getCreadoPor())
                .creadoPorUserId(t.getCreadoPorUserId())
                .asignadoAUserId(t.getAsignadoAUserId())
                .asignadoAUsername(asignado != null ? asignado.getUsername() : null)
                .asignadoANombre(buildFullName(asignado))
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private String normalizeEstado(String estado) {
        if (estado == null) return null;
        String value = estado.trim().toUpperCase(Locale.ROOT);
        return value.isBlank() ? null : value;
    }

    private Usuario resolveAssigneeForManualTask(Long organizationId, Long assigneeUserId, Usuario creator) {
        Long targetUserId = assigneeUserId != null ? assigneeUserId : creator.getId();
        OrganizationMembership membership = organizationMembershipRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario asignado no encontrado"));
        if (!membership.getOrganization().getId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario asignado no pertenece a tu organización");
        }

        Usuario user = membership.getUser();
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede asignar a un usuario inactivo");
        }
        return user;
    }

    private Tarea findVisibleTaskOrThrow(Long tareaId, Long organizationId, Long userId) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        if (!tarea.getOrganizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        boolean visible = tarea.getCreadoPorUserId().equals(userId) || tarea.getAsignadoAUserId().equals(userId);
        if (!visible) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        Usuario actor = usuarioRepository.findById(userId).orElse(null);
        if (tarea.getTipo().equals("ALERTA_STOCK") && !canSeeAlertTasks(actor)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        return tarea;
    }

    private String buildFullName(Usuario usuario) {
        if (usuario == null) return null;
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String apellidos = usuario.getApellidos() == null ? "" : usuario.getApellidos().trim();
        String full = (nombre + " " + apellidos).trim();
        return full.isEmpty() ? usuario.getUsername() : full;
    }

    private boolean canReassign(Usuario actor, Tarea tarea) {
        if (actor == null) return false;
        return actor.getRole() == Usuario.Role.ROLE_ADMIN || actor.getRole() == Usuario.Role.ROLE_MANAGER;
    }

    private boolean canSeeAlertTasks(Usuario user) {
        if (user == null || user.getRole() == null) return false;
        return user.getRole() == Usuario.Role.ROLE_ADMIN || user.getRole() == Usuario.Role.ROLE_MANAGER;
    }

    private boolean isDuplicateAlertAssignmentConstraint(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("uq_tarea_alerta_asignado")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
