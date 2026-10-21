package com.nuv.tfgrs.features.reportes.repository;

import com.nuv.tfgrs.features.reportes.entity.ReporteProgramado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReporteProgramadoRepository extends JpaRepository<ReporteProgramado, Long> {
    List<ReporteProgramado> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    List<ReporteProgramado> findByActivoTrueAndOrganizationIdIsNotNullAndProximaEjecucionLessThanEqual(LocalDateTime now);
    Optional<ReporteProgramado> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByIdAndOrganizationId(Long id, Long organizationId);
}
