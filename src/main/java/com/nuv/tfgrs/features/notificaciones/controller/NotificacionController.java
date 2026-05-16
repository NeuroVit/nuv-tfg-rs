package com.nuv.tfgrs.features.notificaciones.controller;

import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.notificaciones.dto.response.ConteoNoLeidasResponse;
import com.nuv.tfgrs.features.notificaciones.dto.response.NotificacionResponse;
import com.nuv.tfgrs.features.notificaciones.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping("/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<NotificacionResponse>> listarPaginado(
            @RequestParam(name = "soloNoLeidas", defaultValue = "false") Boolean soloNoLeidas,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(notificacionService.listarPaginado(soloNoLeidas, limit, offset));
    }

    @GetMapping("/no-leidas/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConteoNoLeidasResponse> contarNoLeidas() {
        return ResponseEntity.ok(new ConteoNoLeidasResponse(notificacionService.contarNoLeidas()));
    }

    @PatchMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }

    @PatchMapping("/leer-todas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> marcarTodasLeidas() {
        notificacionService.marcarTodasLeidas();
        return ResponseEntity.noContent().build();
    }
}
