package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.OrganizationMembership;
import com.nuv.tfgrs.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    Optional<OrganizationMembership> findByUserId(Long userId);
    Optional<OrganizationMembership> findByUserUsername(String username);
    List<OrganizationMembership> findByOrganizationIdOrderByUserUsernameAsc(Long organizationId);
    boolean existsByUser(Usuario user);
    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);
}
