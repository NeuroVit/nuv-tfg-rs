package com.nuv.tfgrs.scheduler;

import com.nuv.tfgrs.features.reportes.dto.request.CrearReporteRequest;
import com.nuv.tfgrs.features.reportes.entity.ReporteProgramado;
import com.nuv.tfgrs.features.reportes.repository.ReporteProgramadoRepository;
import com.nuv.tfgrs.features.reportes.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteSchedulerTest {

    @Mock
    private ReporteProgramadoRepository reporteProgramadoRepository;

    @Mock
    private ReporteService reporteService;

    @Test
    void debeContinuarConSiguienteReporteSiUnoFalla() {
        LocalDateTime now = LocalDateTime.now();
        ReporteProgramado primero = buildProgramado(1L, "INVENTARIO", "DIARIO", 10L);
        ReporteProgramado segundo = buildProgramado(2L, "VENTAS", "SEMANAL", 10L);

        when(reporteProgramadoRepository
                .findByActivoTrueAndOrganizationIdIsNotNullAndProximaEjecucionLessThanEqual(any(LocalDateTime.class)))
                .thenReturn(List.of(primero, segundo));

        AtomicInteger callCounter = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (callCounter.getAndIncrement() == 0) {
                throw new RuntimeException("fallo IA");
            }
            return null;
        }).when(reporteService).generar(any(CrearReporteRequest.class), any(String.class), any(Long.class));

        ReporteScheduler scheduler = new ReporteScheduler(reporteProgramadoRepository, reporteService);
        scheduler.ejecutarProgramados();

        verify(reporteService, times(2)).generar(any(CrearReporteRequest.class), any(String.class), any(Long.class));

        ArgumentCaptor<ReporteProgramado> captor = ArgumentCaptor.forClass(ReporteProgramado.class);
        verify(reporteProgramadoRepository, times(1)).save(captor.capture());
        assertNotNull(captor.getValue().getUltimaEjecucion());
        assertNotNull(captor.getValue().getProximaEjecucion());
    }

    private ReporteProgramado buildProgramado(Long id, String tipo, String frecuencia, Long organizationId) {
        ReporteProgramado p = new ReporteProgramado();
        p.setId(id);
        p.setTipo(tipo);
        p.setFrecuencia(frecuencia);
        p.setActivo(true);
        p.setOrganizationId(organizationId);
        p.setCreadoPor("sistema");
        p.setProximaEjecucion(LocalDateTime.now().minusHours(1));
        return p;
    }
}
