package com.nuv.tfgrs.features.usuarios.dto.response;

import com.nuv.tfgrs.features.usuarios.entity.Usuario;

import java.time.LocalDateTime;

public record UsuarioAdminResponse(
        Long id,
        String username,
        String email,
        String nombre,
        String apellidos,
        String role,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
    public static UsuarioAdminResponse from(Usuario usuario) {
        return new UsuarioAdminResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getApellidos(),
                usuario.getRole().name(),
                Boolean.TRUE.equals(usuario.getActive()),
                usuario.getCreatedAt(),
                usuario.getUpdatedAt()
        );
    }
}
