package com.nuv.tfgrs.features.proveedores.entity;

import com.nuv.tfgrs.features.productos.entity.Producto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Entity
@Table(name = "providers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 254)
    private String email;

    @Column(name = "warehouse_location", nullable = false, length = 500)
    private String warehouseLocation;

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "provider", fetch = FetchType.LAZY)
    private List<Producto> products;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
