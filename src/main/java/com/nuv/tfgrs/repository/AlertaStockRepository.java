package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.AlertaStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {
    List<AlertaStock> findByOrganizationIdAndResolvedFalseOrderByCreatedAtDesc(Long organizationId);
    List<AlertaStock> findByOrganizationIdAndResolvedTrueOrderByCreatedAtDesc(Long organizationId);
    List<AlertaStock> findByOrganizationIdAndProductIdOrderByCreatedAtDesc(Long organizationId, Long productId);
    List<AlertaStock> findByOrganizationId(Long organizationId);
    java.util.Optional<AlertaStock> findByIdAndOrganizationId(Long id, Long organizationId);
}
