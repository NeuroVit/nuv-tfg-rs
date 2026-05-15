package com.nuv.tfgrs.features.inventario.repository;

import com.nuv.tfgrs.features.inventario.entity.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {
    List<MovimientoStock> findByOrganizationIdAndProductIdOrderByMovedAtDesc(Long organizationId, Long productId);

    @Query("select ms from MovimientoStock ms join fetch ms.product where ms.organizationId = :organizationId order by ms.movedAt desc")
    List<MovimientoStock> findLatestWithProduct(@Param("organizationId") Long organizationId, Pageable pageable);
}
