package com.nuv.tfgrs.dto.response;

import com.nuv.tfgrs.entity.Categoria;

public record CategoriaResponse(
        Long id,
        String nombre
) {
    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getName());
    }
}
