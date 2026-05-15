package com.nuv.tfgrs.features.usuarios.dto.response;

import java.time.LocalDateTime;

public record UsuarioRoleAuditResponse(
        Long id,
        Long userId,
        String username,
        Long changedById,
        String changedByUsername,
        String oldRole,
        String newRole,
        String reason,
        LocalDateTime changedAt
) {
}
