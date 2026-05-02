package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {
    List<MovimientoStock> findByProductIdOrderByMovedAtDesc(Long productId);

    @Query("select ms from MovimientoStock ms join fetch ms.product order by ms.movedAt desc")
    List<MovimientoStock> findLatestWithProduct(Pageable pageable);
}
