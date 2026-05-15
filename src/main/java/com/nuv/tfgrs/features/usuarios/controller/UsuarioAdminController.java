package com.nuv.tfgrs.features.usuarios.controller;

import com.nuv.tfgrs.features.usuarios.dto.request.ActualizarRolUsuarioRequest;
import com.nuv.tfgrs.features.usuarios.dto.request.InvitarUsuarioRequest;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioAdminResponse;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioInviteResponse;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioRoleAuditResponse;
import com.nuv.tfgrs.features.usuarios.service.UsuarioAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UsuarioAdminResponse>> listar() {
        return ResponseEntity.ok(usuarioAdminService.listar());
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UsuarioInviteResponse> invitar(
            @Valid @RequestBody InvitarUsuarioRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioAdminService.invitar(request, authentication.getName()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UsuarioAdminResponse> actualizarRol(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarRolUsuarioRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(usuarioAdminService.actualizarRol(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UsuarioAdminResponse> desactivar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(usuarioAdminService.desactivar(id, authentication.getName()));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<UsuarioAdminResponse> activar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(usuarioAdminService.activar(id, authentication.getName()));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<UsuarioRoleAuditResponse>> auditoria(
            @RequestParam(name = "limit", defaultValue = "100") int limit
    ) {
        return ResponseEntity.ok(usuarioAdminService.auditoria(limit));
    }
}
