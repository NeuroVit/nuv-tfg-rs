package com.nuv.tfgrs.features.categorias.repository;
 
import com.nuv.tfgrs.features.categorias.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
import java.util.Optional;
 

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByOrganizationIdOrderByNameAsc(Long organizationId);
    Optional<Categoria> findByIdAndOrganizationId(Long id, Long organizationId);
    boolean existsByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);
    boolean existsByNameIgnoreCaseAndOrganizationIdAndIdNot(String name, Long organizationId, Long id);
}
