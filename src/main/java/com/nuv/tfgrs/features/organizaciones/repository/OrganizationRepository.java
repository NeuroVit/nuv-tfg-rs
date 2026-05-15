package com.nuv.tfgrs.features.organizaciones.repository;

import com.nuv.tfgrs.features.organizaciones.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsBySlug(String slug);
    Page<Organization> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);
}
