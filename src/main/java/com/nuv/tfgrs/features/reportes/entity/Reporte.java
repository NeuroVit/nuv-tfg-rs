package com.nuv.tfgrs.features.reportes.entity;

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
@Table(name = "reportes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String parametros;

    @Column(columnDefinition = "LONGTEXT")
    private String datos;

    @Column(name = "analisis_ia", columnDefinition = "LONGTEXT")
    private String analisisIa;

    @Column(name = "analisis_ia_estado", nullable = false, length = 30)
    private String analisisIaEstado;

    @Column(name = "analisis_ia_modelo", length = 100)
    private String analisisIaModelo;

    @Column(name = "analisis_ia_generado_en")
    private LocalDateTime analisisIaGeneradoEn;

    @Column(name = "analisis_ia_error", length = 500)
    private String analisisIaError;

    @Column(name = "generado_por", length = 255)
    private String generadoPor;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (analisisIaEstado == null || analisisIaEstado.isBlank()) {
            analisisIaEstado = "NO_DISPONIBLE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
