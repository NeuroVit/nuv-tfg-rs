package com.nuv.tfgrs.features.usuarios.service;

import com.nuv.tfgrs.features.usuarios.dto.request.ActualizarRolUsuarioRequest;
import com.nuv.tfgrs.features.usuarios.dto.request.InvitarUsuarioRequest;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioAdminResponse;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioInviteResponse;
import com.nuv.tfgrs.features.usuarios.dto.response.UsuarioRoleAuditResponse;

import java.util.List;

public interface UsuarioAdminService {
    List<UsuarioAdminResponse> listar();
    UsuarioInviteResponse invitar(InvitarUsuarioRequest request, String actorUsername);
    UsuarioAdminResponse actualizarRol(Long userId, ActualizarRolUsuarioRequest request, String actorUsername);
    UsuarioAdminResponse desactivar(Long userId, String actorUsername);
    UsuarioAdminResponse activar(Long userId, String actorUsername);
    List<UsuarioRoleAuditResponse> auditoria(int limit);
}
