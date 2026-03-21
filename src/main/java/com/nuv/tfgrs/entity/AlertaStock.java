package com.nuv.tfgrs.entity;
 
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
@Entity
@Table(name = "stock_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Producto product;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;
 
    @Column(nullable = false, length = 500)
    private String message;
 
    @Column(nullable = false)
    private Boolean resolved;
 
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        resolved = false;
    }
 
    public enum AlertType {
        LOW_STOCK,
        OUT_OF_STOCK,
        OVERSTOCK,
        PRICE_ANOMALY
    }
}