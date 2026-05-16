package com.nuv.tfgrs.features.usuarios.dto.response;

public record UsuarioInviteResponse(
        UsuarioAdminResponse usuario,
        String temporaryPassword
) {
}
