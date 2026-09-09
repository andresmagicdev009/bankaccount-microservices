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
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * JPA adapter for {@link CustomerRepositoryPort}. Logs at DEBUG level: the
 * business events are already logged by CustomerService, so what is useful
 * here is the row-level detail when a request has to be traced end to end.
 */
@Slf4j
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
        // saveAndFlush rather than save: with GenerationType.UUID the id is assigned
        // in memory, so save() never touches the database and Hibernate defers the
        // INSERT until commit. @CreationTimestamp / @UpdateTimestamp are resolved in
        // that INSERT, so without the flush the mapping back would read createdAt and
        // updatedAt still null and the response would break the contract, which
        // declares them required.
        CustomerEntity persisted = jpaRepository.saveAndFlush(entity);
        log.debug("Customer row persisted: id={}", persisted.getId());
        return mapper.toDomain(persisted);
    }

    @Override
    public Optional<Customer> findById(String id) {
        Optional<Customer> found = jpaRepository.findById(id).map(mapper::toDomain);
        log.debug("findById id={} hit={}", id, found.isPresent());
        return found;
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
        log.debug("Customer row deleted: id={}", id);
    }

    @Override 
    public Page<Customer> findAll(Boolean status, Pageable pageable) {
        Page<CustomerEntity> page = (status == null)
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findByStatus(status, pageable);
        log.debug("findAll status={} page={} size={} totalElements={}",
                status, pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements());
        return page.map(mapper::toDomain);
    }

}
