package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
