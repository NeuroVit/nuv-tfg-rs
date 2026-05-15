package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.entity.AlertaStock.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
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
}
