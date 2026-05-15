package com.nuv.tfgrs.features.reportes.service.impl;

import com.nuv.tfgrs.features.reportes.dto.request.CrearReporteProgramadoRequest;
import com.nuv.tfgrs.features.reportes.dto.response.ReporteProgramadoResponse;
import com.nuv.tfgrs.features.reportes.entity.ReporteProgramado;
import com.nuv.tfgrs.features.reportes.repository.ReporteProgramadoRepository;
import com.nuv.tfgrs.features.reportes.service.ReporteProgramadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteProgramadoServiceImpl implements ReporteProgramadoService {

    private final ReporteProgramadoRepository reporteProgramadoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReporteProgramadoResponse> listar() {
        return reporteProgramadoRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReporteProgramadoResponse crear(CrearReporteProgramadoRequest request, String username) {
        LocalDateTime proximaEjecucion = calcularProxima(LocalDate.now(), request.getFrecuencia());

        ReporteProgramado programado = ReporteProgramado.builder()
                .tipo(request.getTipo())
                .frecuencia(request.getFrecuencia())
                .activo(true)
                .creadoPor(username)
                .proximaEjecucion(proximaEjecucion)
                .build();

        return toResponse(reporteProgramadoRepository.save(programado));
    }

    @Override
    @Transactional
    public ReporteProgramadoResponse toggleActivo(Long id) {
        ReporteProgramado programado = reporteProgramadoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programación no encontrada"));
        programado.setActivo(!Boolean.TRUE.equals(programado.getActivo()));
        return toResponse(reporteProgramadoRepository.save(programado));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!reporteProgramadoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Programación no encontrada");
        }
        reporteProgramadoRepository.deleteById(id);
    }

    private LocalDateTime calcularProxima(LocalDate base, String frecuencia) {
        return switch (frecuencia) {
            case "DIARIO" -> base.plusDays(1).atStartOfDay();
            case "SEMANAL" -> base.plusWeeks(1).atStartOfDay();
            case "MENSUAL" -> base.plusMonths(1).withDayOfMonth(1).atStartOfDay();
            default -> base.plusDays(1).atStartOfDay();
        };
    }

    public static LocalDateTime calcularSiguiente(LocalDateTime ultimaEjecucion, String frecuencia) {
        return switch (frecuencia) {
            case "DIARIO" -> ultimaEjecucion.toLocalDate().plusDays(1).atStartOfDay();
            case "SEMANAL" -> ultimaEjecucion.toLocalDate().plusWeeks(1).atStartOfDay();
            case "MENSUAL" -> ultimaEjecucion.toLocalDate().plusMonths(1).withDayOfMonth(1).atStartOfDay();
            default -> ultimaEjecucion.toLocalDate().plusDays(1).atStartOfDay();
        };
    }

    private ReporteProgramadoResponse toResponse(ReporteProgramado p) {
        return ReporteProgramadoResponse.builder()
                .id(p.getId())
                .tipo(p.getTipo())
                .frecuencia(p.getFrecuencia())
                .activo(p.getActivo())
                .creadoPor(p.getCreadoPor())
                .ultimaEjecucion(p.getUltimaEjecucion())
                .proximaEjecucion(p.getProximaEjecucion())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
