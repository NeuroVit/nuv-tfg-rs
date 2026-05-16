package com.nuv.tfgrs.features.clientes.repository;

import com.nuv.tfgrs.features.clientes.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByOrganizationIdOrderByNameAsc(Long organizationId);
    List<Cliente> findByOrganizationIdAndActiveTrueOrderByNameAsc(Long organizationId);
    Optional<Cliente> findByOrganizationIdAndEmail(Long organizationId, String email);
    boolean existsByOrganizationIdAndEmailAndIdNot(Long organizationId, String email, Long id);
    boolean existsByIdAndOrganizationIdAndActiveTrue(Long id, Long organizationId);
    Optional<Cliente> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query(
            value = """
                    SELECT c
                    FROM Cliente c
                    WHERE c.organizationId = :organizationId
                      AND (
                            :status IS NULL
                            OR (:status = 'ACTIVE' AND c.active = true)
                            OR (:status = 'ARCHIVED' AND c.active = false)
                      )
                      AND (
                            :search IS NULL
                            OR LOWER(c.name) LIKE :search
                            OR LOWER(COALESCE(c.email, '')) LIKE :search
                            OR LOWER(COALESCE(c.phone, '')) LIKE :search
                      )
                    ORDER BY c.createdAt DESC, c.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(c)
                    FROM Cliente c
                    WHERE c.organizationId = :organizationId
                      AND (
                            :status IS NULL
                            OR (:status = 'ACTIVE' AND c.active = true)
                            OR (:status = 'ARCHIVED' AND c.active = false)
                      )
                      AND (
                            :search IS NULL
                            OR LOWER(c.name) LIKE :search
                            OR LOWER(COALESCE(c.email, '')) LIKE :search
                            OR LOWER(COALESCE(c.phone, '')) LIKE :search
                      )
                    """
    )
    Page<Cliente> findPagedByFilters(
            @Param("organizationId") Long organizationId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable
    );
}
