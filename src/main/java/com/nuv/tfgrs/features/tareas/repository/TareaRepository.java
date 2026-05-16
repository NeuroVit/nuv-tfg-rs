package com.nuv.tfgrs.features.tareas.repository;

import com.nuv.tfgrs.features.tareas.entity.Tarea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {
    @Query("""
            SELECT t
            FROM Tarea t
            WHERE t.organizationId = :organizationId
              AND (:estado IS NULL OR t.estado = :estado)
              AND (:includeAlertTasks = true OR t.tipo <> 'ALERTA_STOCK')
              AND (t.creadoPorUserId = :userId OR t.asignadoAUserId = :userId)
            ORDER BY t.createdAt DESC
            """)
    List<Tarea> findVisibleByOrganizationAndEstado(
            @Param("organizationId") Long organizationId,
            @Param("userId") Long userId,
            @Param("estado") String estado,
            @Param("includeAlertTasks") boolean includeAlertTasks
    );

    @Query("""
            SELECT t
            FROM Tarea t
            WHERE t.organizationId = :organizationId
              AND (:estado IS NULL OR t.estado = :estado)
              AND (:includeAlertTasks = true OR t.tipo <> 'ALERTA_STOCK')
              AND (t.creadoPorUserId = :userId OR t.asignadoAUserId = :userId)
            ORDER BY t.createdAt DESC
            """)
    Page<Tarea> findVisibleByOrganizationAndEstado(
            @Param("organizationId") Long organizationId,
            @Param("userId") Long userId,
            @Param("estado") String estado,
            @Param("includeAlertTasks") boolean includeAlertTasks,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(t)
            FROM Tarea t
            WHERE t.organizationId = :organizationId
              AND (:includeAlertTasks = true OR t.tipo <> 'ALERTA_STOCK')
              AND (t.creadoPorUserId = :userId OR t.asignadoAUserId = :userId)
            """)
    long countVisible(
            @Param("organizationId") Long organizationId,
            @Param("userId") Long userId,
            @Param("includeAlertTasks") boolean includeAlertTasks
    );

    @Query("""
            SELECT COUNT(t)
            FROM Tarea t
            WHERE t.organizationId = :organizationId
              AND t.estado = :estado
              AND (:includeAlertTasks = true OR t.tipo <> 'ALERTA_STOCK')
              AND (t.creadoPorUserId = :userId OR t.asignadoAUserId = :userId)
            """)
    long countVisibleByEstado(
            @Param("organizationId") Long organizationId,
            @Param("userId") Long userId,
            @Param("estado") String estado,
            @Param("includeAlertTasks") boolean includeAlertTasks
    );

    boolean existsByOrganizationIdAndAlertaIdAndAsignadoAUserId(Long organizationId, Long alertaId, Long asignadoAUserId);

    List<Tarea> findByOrganizationIdAndTipoAndAlertaIdIsNotNullAndEstadoNot(Long organizationId, String tipo, String estado);
}
