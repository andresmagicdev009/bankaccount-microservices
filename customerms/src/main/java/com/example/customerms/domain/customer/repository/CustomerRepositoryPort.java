package com.example.customerms.domain.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.customerms.domain.customer.entity.Customer;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findById(String id);
    
    List<Customer> findAll();

    Optional<Customer> findByIdentification(String identification);

    boolean existsByIdentification(String identification);

    void deleteById(String id);

    Page<Customer> findAll(Boolean status, Pageable pageable);
}
