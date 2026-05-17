package com.nuv.tfgrs.features.reportes.repository;

import com.nuv.tfgrs.features.reportes.entity.ReporteProgramado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReporteProgramadoRepository extends JpaRepository<ReporteProgramado, Long> {
    List<ReporteProgramado> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    List<ReporteProgramado> findByActivoTrueAndOrganizationIdIsNotNullAndProximaEjecucionLessThanEqual(LocalDateTime now);
    Optional<ReporteProgramado> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByIdAndOrganizationId(Long id, Long organizationId);
}
