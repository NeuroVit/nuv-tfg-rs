package com.nuv.tfgrs.features.organizaciones.repository;

import com.nuv.tfgrs.features.organizaciones.entity.OrganizationMembership;
import com.nuv.tfgrs.features.usuarios.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    Optional<OrganizationMembership> findByUserId(Long userId);
    Optional<OrganizationMembership> findByUserUsername(String username);
    List<OrganizationMembership> findByOrganizationIdOrderByUserUsernameAsc(Long organizationId);
    @Query("""
            SELECT om
            FROM OrganizationMembership om
            WHERE om.organization.id = :organizationId
              AND om.user.active = true
              AND om.user.role IN :roles
            ORDER BY om.user.username ASC
            """)
    List<OrganizationMembership> findActiveByOrganizationIdAndUserRoleIn(
            @Param("organizationId") Long organizationId,
            @Param("roles") List<Usuario.Role> roles
    );
    boolean existsByUser(Usuario user);
    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
}
