package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.AlertaStock;
import com.nuv.tfgrs.entity.AlertaStock.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {
    List<AlertaStock> findByResolvedFalseOrderByCreatedAtDesc();
    List<AlertaStock> findByResolvedTrueOrderByCreatedAtDesc();
    List<AlertaStock> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<AlertaStock> findByProductIdAndResolvedFalse(Long productId);
    boolean existsByProductIdAndAlertTypeAndResolvedFalse(Long productId, AlertType alertType);
}
