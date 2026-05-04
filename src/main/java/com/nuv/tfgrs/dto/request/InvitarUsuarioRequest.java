package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitarUsuarioRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 50) String nombre,
        @NotBlank @Size(min = 2, max = 100) String apellidos,
        @NotBlank String role
) {
}
