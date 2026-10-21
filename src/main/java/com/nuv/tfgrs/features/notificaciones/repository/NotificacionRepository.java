package com.nuv.tfgrs.features.notificaciones.repository;

import com.nuv.tfgrs.features.notificaciones.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("""
            SELECT n
            FROM Notificacion n
            WHERE n.organizationId = :organizationId
              AND n.recipientUserId = :recipientUserId
              AND (:soloNoLeidas = false OR n.leida = false)
            ORDER BY n.createdAt DESC
            """)
    Page<Notificacion> findVisibleByRecipient(
            @Param("organizationId") Long organizationId,
            @Param("recipientUserId") Long recipientUserId,
            @Param("soloNoLeidas") boolean soloNoLeidas,
            Pageable pageable
    );

    Optional<Notificacion> findByIdAndOrganizationIdAndRecipientUserId(Long id, Long organizationId, Long recipientUserId);

    long countByOrganizationIdAndRecipientUserIdAndLeidaFalse(Long organizationId, Long recipientUserId);

    @Modifying
    @Query("""
            UPDATE Notificacion n
               SET n.leida = true,
                   n.readAt = :readAt
             WHERE n.organizationId = :organizationId
               AND n.recipientUserId = :recipientUserId
               AND n.leida = false
            """)
    int markAllAsRead(
            @Param("organizationId") Long organizationId,
            @Param("recipientUserId") Long recipientUserId,
            @Param("readAt") LocalDateTime readAt
    );
}
