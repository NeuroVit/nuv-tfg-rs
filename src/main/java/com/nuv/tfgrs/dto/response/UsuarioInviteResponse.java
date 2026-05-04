package com.nuv.tfgrs.dto.response;

public record UsuarioInviteResponse(
        UsuarioAdminResponse usuario,
        String temporaryPassword
) {
}
