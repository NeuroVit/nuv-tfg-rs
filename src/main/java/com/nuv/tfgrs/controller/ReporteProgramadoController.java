package com.nuv.tfgrs.controller;

import com.nuv.tfgrs.dto.request.CrearReporteProgramadoRequest;
import com.nuv.tfgrs.dto.response.ReporteProgramadoResponse;
import com.nuv.tfgrs.service.ReporteProgramadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reportes/programados")
@RequiredArgsConstructor
public class ReporteProgramadoController {

    private final ReporteProgramadoService reporteProgramadoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReporteProgramadoResponse>> listar() {
        return ResponseEntity.ok(reporteProgramadoService.listar());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ReporteProgramadoResponse> crear(
            @Valid @RequestBody CrearReporteProgramadoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reporteProgramadoService.crear(request, authentication.getName()));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ReporteProgramadoResponse> toggleActivo(@PathVariable Long id) {
        return ResponseEntity.ok(reporteProgramadoService.toggleActivo(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        reporteProgramadoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
