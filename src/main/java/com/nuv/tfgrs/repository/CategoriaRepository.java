package com.nuv.tfgrs.repository;
 
import com.nuv.tfgrs.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
 
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByOrganizationIdOrderByNameAsc(Long organizationId);
    Optional<Categoria> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);
    boolean existsByNameIgnoreCaseAndOrganizationIdAndIdNot(String name, Long organizationId, Long id);
}
