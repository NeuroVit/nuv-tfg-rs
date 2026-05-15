package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.ActualizarEstadoTareaRequest;
import com.nuv.tfgrs.dto.request.CrearTareaRequest;
import com.nuv.tfgrs.dto.response.TareaResponse;
import com.nuv.tfgrs.service.TareaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaResponse> crear(
            @Valid @RequestBody CrearTareaRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tareaService.crear(request, authentication.getName()));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TareaResponse> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEstadoTareaRequest request
    ) {
        return ResponseEntity.ok(tareaService.actualizarEstado(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        tareaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
