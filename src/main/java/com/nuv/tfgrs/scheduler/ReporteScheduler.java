package com.nuv.tfgrs.scheduler;

import com.nuv.tfgrs.dto.request.CrearReporteRequest;
import com.nuv.tfgrs.entity.ReporteProgramado;
import com.nuv.tfgrs.repository.ReporteProgramadoRepository;
import com.nuv.tfgrs.service.ReporteService;
import com.nuv.tfgrs.service.impl.ReporteProgramadoServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReporteScheduler {

    private final ReporteProgramadoRepository reporteProgramadoRepository;
    private final ReporteService reporteService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void ejecutarProgramados() {
        LocalDateTime now = LocalDateTime.now();
        List<ReporteProgramado> pending =
                reporteProgramadoRepository.findByActivoTrueAndProximaEjecucionLessThanEqual(now);

        if (pending.isEmpty()) return;

        log.info("Ejecutando {} reportes programados", pending.size());

        for (ReporteProgramado programado : pending) {
            try {
                String fecha = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);

                CrearReporteRequest req = new CrearReporteRequest();
                req.setNombre("Reporte " + programado.getTipo() + " — " + fecha);
                req.setTipo(programado.getTipo());
                req.setDescripcion("Generado automáticamente (" + programado.getFrecuencia().toLowerCase() + ")");

                reporteService.generar(req, programado.getCreadoPor() != null ? programado.getCreadoPor() : "Sistema");

                programado.setUltimaEjecucion(now);
                programado.setProximaEjecucion(
                        ReporteProgramadoServiceImpl.calcularSiguiente(now, programado.getFrecuencia()));
                reporteProgramadoRepository.save(programado);

                log.info("Reporte programado generado: tipo={}, frecuencia={}", programado.getTipo(), programado.getFrecuencia());
            } catch (Exception e) {
                log.error("Error generando reporte programado id={}: {}", programado.getId(), e.getMessage());
            }
        }
    }
}
