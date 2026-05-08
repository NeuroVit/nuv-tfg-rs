package com.nuv.tfgrs.dto.response;

import com.nuv.tfgrs.entity.Usuario;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String nombre,
        String apellidos,
        String role,
        boolean activo,
        String avatarUrl,
        boolean mustChangePassword
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
                Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }
}
