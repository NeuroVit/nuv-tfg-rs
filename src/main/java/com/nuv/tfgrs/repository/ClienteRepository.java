package com.nuv.tfgrs.repository;

import com.nuv.tfgrs.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findAllByOrderByNameAsc();
    List<Cliente> findByActiveTrueOrderByNameAsc();
    Optional<Cliente> findByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
