package com.nuv.tfgrs.dto.request;

import jakarta.validation.constraints.Size;

public record EditarCategoriaRequest(
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion
) {}
