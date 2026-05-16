package com.nuv.tfgrs.features.tareas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tareas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 255)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** ALERTA_STOCK | MANUAL */
    @Column(nullable = false, length = 30)
    private String tipo;

    /** PENDIENTE | EN_GESTION | POSPUESTO | COMPLETADO */
    @Column(nullable = false, length = 30)
    private String estado;

    /** ALTA | MEDIA | BAJA */
    @Column(nullable = false, length = 20)
    private String prioridad;

    @Column(name = "alerta_id")
    private Long alertaId;

    @Column(name = "producto_nombre", length = 255)
    private String productoNombre;

    @Column(length = 100)
    private String sku;

    @Column(name = "alerta_tipo", length = 50)
    private String alertaTipo;

    @Column(name = "creado_por", length = 255)
    private String creadoPor;

    @Column(name = "creado_por_user_id", nullable = false)
    private Long creadoPorUserId;

    @Column(name = "asignado_a_user_id", nullable = false)
    private Long asignadoAUserId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (estado == null) estado = "PENDIENTE";
        if (prioridad == null) prioridad = "MEDIA";
        if (tipo == null) tipo = "MANUAL";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
