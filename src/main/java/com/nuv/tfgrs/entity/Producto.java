package com.nuv.tfgrs.entity;
 
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
 
@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
 
    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;
 
    @NotBlank
    @Column(nullable = false, length = 100)
    private String sku;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
    @Min(0)
    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;
 
    @Min(0)
    @Column(name = "minimum_stock_level", nullable = false)
    private Integer minimumStockLevel;
 
    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "acquisition_price", precision = 10, scale = 2)
    private BigDecimal acquisitionPrice;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Categoria category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Proveedor provider;
 
    // Optimistic locking — incremented automatically by Hibernate on every update
    @Version
    private Long version;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
 
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<MovimientoStock> stockMovements;
 
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<HistorialPrecio> priceHistory;
 
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
