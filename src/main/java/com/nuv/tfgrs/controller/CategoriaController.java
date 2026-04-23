package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.response.CategoriaResponse;
import com.nuv.tfgrs.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaRepository categoriaRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CategoriaResponse>> listarTodas() {
        List<CategoriaResponse> categorias = categoriaRepository.findAll()
                .stream()
                .map(CategoriaResponse::from)
                .toList();
        return ResponseEntity.ok(categorias);
    }
}
