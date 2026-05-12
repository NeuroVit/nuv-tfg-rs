package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByIdAndOrganizationId(Long id, Long organizationId);
    Page<Producto> findByOrganizationIdAndCategoryId(Long organizationId, Long categoryId, Pageable pageable);
    List<Producto> findByOrganizationId(Long organizationId);
    @Query("SELECT p FROM Producto p WHERE p.organizationId = :organizationId AND p.currentQuantity < p.minimumStockLevel")
    List<Producto> findLowStockProductsByOrganizationId(@Param("organizationId") Long organizationId);
    boolean existsBySkuAndOrganizationId(String sku, Long organizationId);
    boolean existsBySkuAndOrganizationIdAndIdNot(String sku, Long organizationId, Long id);
    long countByProviderIdAndOrganizationId(Long providerId, Long organizationId);
    List<Producto> findByProviderIdAndOrganizationIdOrderByNameAsc(Long providerId, Long organizationId);
    @Query("SELECT p FROM Producto p WHERE p.organizationId = :organizationId AND p.id IN :ids")
    List<Producto> findAllByIdInAndOrganizationId(@Param("ids") Iterable<Long> ids, @Param("organizationId") Long organizationId);

    @Query("""
            SELECT p
            FROM Producto p
            WHERE p.organizationId = :organizationId
              AND p.acquisitionPrice IS NOT NULL
              AND p.unitPrice < p.acquisitionPrice
            """)
    List<Producto> findPriceAnomalyProductsByOrganizationId(@Param("organizationId") Long organizationId);
}
