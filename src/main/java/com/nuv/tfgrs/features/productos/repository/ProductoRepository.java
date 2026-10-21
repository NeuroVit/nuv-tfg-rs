package com.nuv.tfgrs.features.productos.repository;

import com.nuv.tfgrs.features.productos.entity.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


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
              AND p.unitPrice <= p.acquisitionPrice
            """)
    List<Producto> findPriceAnomalyProductsByOrganizationId(@Param("organizationId") Long organizationId);

    @Query("SELECT p FROM Producto p WHERE p.currentQuantity < p.minimumStockLevel")
    List<Producto> findLowStockProducts();

    @EntityGraph(attributePaths = {"category", "provider"})
    @Query(
            value = """
                    SELECT p
                    FROM Producto p
                    JOIN p.category c
                    JOIN p.provider pr
                    WHERE p.organizationId = :organizationId
                      AND (:categoryId IS NULL OR c.id = :categoryId)
                      AND (:providerId IS NULL OR pr.id = :providerId)
                      AND (
                            :status IS NULL
                            OR (:status = 'LOW' AND p.currentQuantity > 0 AND p.currentQuantity < p.minimumStockLevel)
                            OR (:status = 'ANOMALY'
                                AND (
                                    (p.acquisitionPrice IS NOT NULL AND p.unitPrice <= p.acquisitionPrice)
                                    OR p.currentQuantity > p.maximumStockLevel
                                )
                            )
                            OR (:status = 'NORMAL'
                                AND p.currentQuantity >= p.minimumStockLevel
                                AND p.currentQuantity <= p.maximumStockLevel
                                AND (p.acquisitionPrice IS NULL OR p.unitPrice > p.acquisitionPrice)
                            )
                      )
                      AND (
                            :search IS NULL
                            OR LOWER(p.name) LIKE :search
                            OR LOWER(p.sku) LIKE :search
                            OR LOWER(c.name) LIKE :search
                            OR LOWER(pr.name) LIKE :search
                      )
                    ORDER BY p.createdAt DESC, p.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(p)
                    FROM Producto p
                    JOIN p.category c
                    JOIN p.provider pr
                    WHERE p.organizationId = :organizationId
                      AND (:categoryId IS NULL OR c.id = :categoryId)
                      AND (:providerId IS NULL OR pr.id = :providerId)
                      AND (
                            :status IS NULL
                            OR (:status = 'LOW' AND p.currentQuantity > 0 AND p.currentQuantity < p.minimumStockLevel)
                            OR (:status = 'ANOMALY'
                                AND (
                                    (p.acquisitionPrice IS NOT NULL AND p.unitPrice <= p.acquisitionPrice)
                                    OR p.currentQuantity > p.maximumStockLevel
                                )
                            )
                            OR (:status = 'NORMAL'
                                AND p.currentQuantity >= p.minimumStockLevel
                                AND p.currentQuantity <= p.maximumStockLevel
                                AND (p.acquisitionPrice IS NULL OR p.unitPrice > p.acquisitionPrice)
                            )
                      )
                      AND (
                            :search IS NULL
                            OR LOWER(p.name) LIKE :search
                            OR LOWER(p.sku) LIKE :search
                            OR LOWER(c.name) LIKE :search
                            OR LOWER(pr.name) LIKE :search
                      )
                    """
    )
    Page<Producto> findPagedByFilters(
            @Param("organizationId") Long organizationId,
            @Param("categoryId") Long categoryId,
            @Param("providerId") Long providerId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}
