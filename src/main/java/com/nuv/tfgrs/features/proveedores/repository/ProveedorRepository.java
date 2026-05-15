package com.nuv.tfgrs.features.proveedores.repository;

import com.nuv.tfgrs.features.proveedores.entity.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByOrganizationIdAndActiveTrueOrderByNameAsc(Long organizationId);
    List<Proveedor> findByOrganizationIdOrderByNameAsc(Long organizationId);
    Optional<Proveedor> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);
    boolean existsByNameIgnoreCaseAndOrganizationIdAndIdNot(String name, Long organizationId, Long id);
    boolean existsByIdAndOrganizationId(Long id, Long organizationId);

    @Query(
            value = """
                    SELECT p
                    FROM Proveedor p
                    WHERE p.organizationId = :organizationId
                      AND (
                            :status IS NULL
                            OR (:status = 'ACTIVE' AND p.active = true)
                            OR (:status = 'ARCHIVED' AND p.active = false)
                      )
                      AND (
                            :search IS NULL
                            OR LOWER(p.name) LIKE :search
                            OR LOWER(p.warehouseLocation) LIKE :search
                      )
                    ORDER BY p.createdAt DESC, p.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(p)
                    FROM Proveedor p
                    WHERE p.organizationId = :organizationId
                      AND (
                            :status IS NULL
                            OR (:status = 'ACTIVE' AND p.active = true)
                            OR (:status = 'ARCHIVED' AND p.active = false)
                      )
                      AND (
                            :search IS NULL
                            OR LOWER(p.name) LIKE :search
                            OR LOWER(p.warehouseLocation) LIKE :search
                      )
                    """
    )
    Page<Proveedor> findPagedByFilters(
            @Param("organizationId") Long organizationId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}
