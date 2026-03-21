package com.nuv.tfgrs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Producto product;

    @Column(nullable = false)
    private Integer delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column(length = 500)
    private String notes;

    @Column(name = "moved_at", updatable = false)
    private LocalDateTime movedAt;

    @PrePersist
    protected void onCreate() {
        movedAt = LocalDateTime.now();
    }

    public enum Reason {
        SALE,
        PURCHASE,
        ADJUSTMENT,
        RETURN,
        DAMAGE,
        INITIAL
    }
}