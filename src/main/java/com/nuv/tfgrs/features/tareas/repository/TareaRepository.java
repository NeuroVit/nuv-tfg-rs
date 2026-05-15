package com.nuv.tfgrs.features.tareas.repository;

import com.nuv.tfgrs.features.tareas.entity.Tarea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findAllByOrderByCreatedAtDesc();
    List<Tarea> findByEstadoOrderByCreatedAtDesc(String estado);
    Page<Tarea> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Tarea> findByEstadoOrderByCreatedAtDesc(String estado, Pageable pageable);
    long countByEstado(String estado);
    boolean existsByAlertaId(Long alertaId);
}
