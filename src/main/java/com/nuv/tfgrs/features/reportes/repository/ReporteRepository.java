package com.nuv.tfgrs.features.reportes.repository;

import com.nuv.tfgrs.features.reportes.entity.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    List<Reporte> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    List<Reporte> findByOrganizationIdAndTipoOrderByCreatedAtDesc(Long organizationId, String tipo);
    Optional<Reporte> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByIdAndOrganizationId(Long id, Long organizationId);
}
