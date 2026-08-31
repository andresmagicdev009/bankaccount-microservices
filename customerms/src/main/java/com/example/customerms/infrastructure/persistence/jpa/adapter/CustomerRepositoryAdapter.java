package com.example.customerms.infrastructure.persistence.jpa.adapter;

import java.util.List;
import java.util.Optional;

import com.example.customerms.domain.customer.entity.Customer;
import com.example.customerms.domain.customer.exception.CustomerNotFoundException;
import com.example.customerms.domain.customer.repository.CustomerRepositoryPort;
import com.example.customerms.infrastructure.persistence.jpa.entity.CustomerEntity;
import com.example.customerms.infrastructure.persistence.jpa.mapper.CustomerPersistenceMapper;
import com.example.customerms.infrastructure.persistence.jpa.repository.JpaCustomerRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerRepositoryAdapter implements CustomerRepositoryPort {

    private final JpaCustomerRepository jpaRepository;
    private final CustomerPersistenceMapper mapper;

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity;
        if (customer.getId() == null) {
            entity = mapper.toEntity(customer);
        } else {
            entity = jpaRepository.findById(customer.getId())
                    .orElseThrow(() -> new CustomerNotFoundException(customer.getId()));
            mapper.updateEntity(entity, customer);
        }
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Customer> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Customer> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Customer> findByIdentification(String identification) {
        return jpaRepository.findByIdentification(identification).map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdentification(String identification) {
        return jpaRepository.existsByIdentification(identification);
    }

    @Override
    public void deleteById(String id) {
        if (!jpaRepository.existsById(id)) {
            throw new CustomerNotFoundException(id);
        }
        jpaRepository.deleteById(id);
    }
}
