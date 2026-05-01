package com.nuv.tfgrs.dto.response;

import com.nuv.tfgrs.entity.Categoria;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion
) {
    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getName(), categoria.getDescription());
    }
}
