package com.nuv.tfgrs.entity;
 
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
import java.util.List;
 
@Entity
@Table(name = "categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true, length = 100)
    private String name;
 
    @Column(length = 500)
    private String description;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Producto> products;
 
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}