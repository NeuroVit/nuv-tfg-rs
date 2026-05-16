package com.nuv.tfgrs.features.tareas.controller;

import com.nuv.tfgrs.features.tareas.dto.request.ActualizarEstadoTareaRequest;
import com.nuv.tfgrs.features.tareas.dto.request.ActualizarAsignadoTareaRequest;
import com.nuv.tfgrs.features.tareas.dto.request.CrearTareaRequest;
import com.nuv.tfgrs.features.common.dto.response.PagedResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaAsignableUsuarioResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResumenResponse;
import com.nuv.tfgrs.features.tareas.dto.response.TareaResponse;
import com.nuv.tfgrs.features.tareas.service.TareaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
public class TareaController {

    private final TareaService tareaService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TareaResponse>> listar(
            @RequestParam(name = "estado", required = false) String estado
    ) {
        return ResponseEntity.ok(tareaService.listar(estado));
    }

    @GetMapping("/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<TareaResponse>> listarPaginado(
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "limit", defaultValue = "9") Integer limit,
            @RequestParam(name = "offset", defaultValue = "0") Integer offset
    ) {
        return ResponseEntity.ok(tareaService.listarPaginado(estado, limit, offset));
    }

    @GetMapping("/resumen")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaResumenResponse> resumen() {
        return ResponseEntity.ok(tareaService.resumen());
    }

    @GetMapping("/asignables")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TareaAsignableUsuarioResponse>> listarAsignables() {
        return ResponseEntity.ok(tareaService.listarAsignables());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaResponse> crear(
            @Valid @RequestBody CrearTareaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tareaService.crear(request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaResponse> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoTareaRequest request
    ) {
        return ResponseEntity.ok(tareaService.actualizarEstado(id, request));
    }

    @PatchMapping("/{id}/asignado")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TareaResponse> actualizarAsignado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarAsignadoTareaRequest request
    ) {
        return ResponseEntity.ok(tareaService.actualizarAsignado(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tareaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
