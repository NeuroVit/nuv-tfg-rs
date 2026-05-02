package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    public record Register(
        @NotBlank @Size(min = 3, max = 50)
        String username,

        @NotBlank @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "La contraseña debe incluir al menos una mayúscula y un número"
        )
        String password,

        @NotBlank
        String nombre,

        @NotBlank
        String apellidos
    ) {}

    public record Login(
        @NotBlank
        String username,

        @NotBlank
        String password
    ) {}

    public record GoogleAuthRequest(String credential) {}

    public record SwitchRoleRequest(
        @NotBlank
        String role
    ) {}
}
