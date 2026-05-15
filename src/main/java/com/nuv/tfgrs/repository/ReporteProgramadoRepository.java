package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.ReporteProgramado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReporteProgramadoRepository extends JpaRepository<ReporteProgramado, Long> {
    List<ReporteProgramado> findAllByOrderByCreatedAtDesc();
    List<ReporteProgramado> findByActivoTrueAndProximaEjecucionLessThanEqual(LocalDateTime now);
}
