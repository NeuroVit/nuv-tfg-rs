package com.nuv.tfgrs.features.auth.dto.response;

import com.nuv.tfgrs.features.usuarios.entity.Usuario;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String nombre,
        String apellidos,
        String role,
        boolean activo,
        String avatarUrl,
        boolean mustChangePassword,
        String timeZone
) {
    public static UserProfileResponse from(Usuario user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNombre(),
                user.getApellidos(),
                user.getRole().name(),
                Boolean.TRUE.equals(user.getActive()),
                user.getAvatarUrl(),
                Boolean.TRUE.equals(user.getMustChangePassword()),
                user.getTimeZone()
        );
    }
}
