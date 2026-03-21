package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findBySku(String sku);
    Page<Producto> findByCategoryId(Long categoryId, Pageable pageable);
    @Query("SELECT p FROM Producto p WHERE p.currentQuantity < p.minimumStockLevel")
    List<Producto> findLowStockProducts();
    boolean existsBySku(String sku);
}