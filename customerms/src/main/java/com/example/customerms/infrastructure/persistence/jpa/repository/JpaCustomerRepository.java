package com.example.customerms.infrastructure.persistence.jpa.repository;

import java.util.Optional;

import com.example.customerms.infrastructure.persistence.jpa.entity.CustomerEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCustomerRepository extends JpaRepository<CustomerEntity, String> {

    Optional<CustomerEntity> findByIdentification(String identification);

    boolean existsByIdentification(String identification);
    Page<CustomerEntity> findByStatus(Boolean status, Pageable pageable);
}
