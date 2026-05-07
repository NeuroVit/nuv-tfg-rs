package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
