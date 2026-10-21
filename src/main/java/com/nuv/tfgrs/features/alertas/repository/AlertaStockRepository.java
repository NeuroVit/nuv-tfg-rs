package com.nuv.tfgrs.features.alertas.repository;

import com.nuv.tfgrs.features.alertas.entity.AlertaStock;
import com.nuv.tfgrs.features.alertas.entity.AlertaStock.AlertType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {
    List<AlertaStock> findByOrganizationIdAndResolvedFalseOrderByCreatedAtDesc(Long organizationId);
    List<AlertaStock> findByOrganizationIdAndResolvedTrueOrderByCreatedAtDesc(Long organizationId);
    List<AlertaStock> findByOrganizationIdAndAlertTypeAndResolvedFalseOrderByCreatedAtDesc(
            Long organizationId,
            AlertType alertType
    );
    List<AlertaStock> findByOrganizationIdAndProductIdOrderByCreatedAtDesc(Long organizationId, Long productId);
    List<AlertaStock> findByOrganizationIdAndProductIdAndResolvedFalse(Long organizationId, Long productId);
    boolean existsByOrganizationIdAndProductIdAndAlertTypeAndResolvedFalse(
            Long organizationId,
            Long productId,
            AlertType alertType
    );
    List<AlertaStock> findByOrganizationId(Long organizationId);
    java.util.Optional<AlertaStock> findByIdAndOrganizationId(Long id, Long organizationId);
    List<AlertaStock> findByResolvedFalseOrderByCreatedAtDesc();

    long countByOrganizationIdAndResolvedFalse(Long organizationId);
    long countByOrganizationIdAndResolvedFalseAndAlertType(Long organizationId, AlertType alertType);
    long countByOrganizationIdAndResolvedFalseAndAlertTypeIn(Long organizationId, Collection<AlertType> alertTypes);
    long countByOrganizationIdAndResolvedTrueAndResolvedAtGreaterThanEqual(Long organizationId, LocalDateTime from);
    @Query("""
            SELECT COUNT(DISTINCT a.product.id)
            FROM AlertaStock a
            WHERE a.organizationId = :organizationId
              AND a.resolved = false
              AND a.alertType = :alertType
            """)
    long countDistinctOpenProductsByType(
            @Param("organizationId") Long organizationId,
            @Param("alertType") AlertType alertType
    );

    @Query("""
            SELECT COUNT(DISTINCT a.product.id)
            FROM AlertaStock a
            WHERE a.organizationId = :organizationId
              AND a.resolved = false
              AND a.alertType IN :alertTypes
            """)
    long countDistinctOpenProductsByTypes(
            @Param("organizationId") Long organizationId,
            @Param("alertTypes") Collection<AlertType> alertTypes
    );

    @EntityGraph(attributePaths = {"product"})
    @Query(
            value = """
                    SELECT a
                    FROM AlertaStock a
                    JOIN a.product p
                    WHERE a.organizationId = :organizationId
                      AND (:resolved IS NULL OR a.resolved = :resolved)
                      AND (
                            :search IS NULL
                            OR LOWER(p.name) LIKE :search
                            OR LOWER(p.sku) LIKE :search
                            OR LOWER(a.message) LIKE :search
                            OR LOWER(str(a.alertType)) LIKE :search
                      )
                    ORDER BY a.createdAt DESC, a.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(a)
                    FROM AlertaStock a
                    JOIN a.product p
                    WHERE a.organizationId = :organizationId
                      AND (:resolved IS NULL OR a.resolved = :resolved)
                      AND (
                            :search IS NULL
                            OR LOWER(p.name) LIKE :search
                            OR LOWER(p.sku) LIKE :search
                            OR LOWER(a.message) LIKE :search
                            OR LOWER(str(a.alertType)) LIKE :search
                      )
                    """
    )
    Page<AlertaStock> findPagedByFilters(
            @Param("organizationId") Long organizationId,
            @Param("resolved") Boolean resolved,
            @Param("search") String search,
            Pageable pageable
    );
}
