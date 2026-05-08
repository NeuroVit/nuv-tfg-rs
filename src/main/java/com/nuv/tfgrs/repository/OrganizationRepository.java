package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    boolean existsBySlug(String slug);
}
