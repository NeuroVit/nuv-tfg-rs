package com.nuv.tfgrs.service;

import com.nuv.tfgrs.dto.request.CrearCategoriaRequest;
import com.nuv.tfgrs.dto.request.EditarCategoriaRequest;
import com.nuv.tfgrs.dto.response.CategoriaResponse;

import java.util.List;

public interface CategoriaService {
    List<CategoriaResponse> listarTodas();
    CategoriaResponse buscarPorId(Long id);
    CategoriaResponse crear(CrearCategoriaRequest request);
    CategoriaResponse actualizar(Long id, EditarCategoriaRequest request);
    void eliminar(Long id);
}
