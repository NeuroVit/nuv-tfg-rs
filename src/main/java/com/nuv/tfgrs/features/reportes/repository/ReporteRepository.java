package com.nuv.tfgrs.features.reportes.repository;

import com.nuv.tfgrs.features.reportes.entity.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    List<Reporte> findAllByOrderByCreatedAtDesc();
    List<Reporte> findByTipoOrderByCreatedAtDesc(String tipo);
}
