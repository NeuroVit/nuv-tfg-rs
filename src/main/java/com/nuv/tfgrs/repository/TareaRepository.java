package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    List<Tarea> findAllByOrderByCreatedAtDesc();
    List<Tarea> findByEstadoOrderByCreatedAtDesc(String estado);
    boolean existsByAlertaId(Long alertaId);
}
