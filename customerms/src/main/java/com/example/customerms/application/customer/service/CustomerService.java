package com.example.customerms.application.customer.service;

import com.example.customerms.domain.customer.entity.Customer;
import com.example.customerms.domain.customer.exception.CustomerNotFoundException;
import com.example.customerms.domain.customer.exception.DuplicateIdentificationException;
import com.example.customerms.domain.customer.repository.CustomerRepositoryPort;
import com.example.customerms.interfaces.rest.dto.CustomerPatchDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Use cases for customers. Blocking on purpose: the reactive boundary lives in
 * the controller, which keeps @Transactional working on a single thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepositoryPort repository;

    // TODO F1: create, findById, findAll, update, patch, delete
    @Transactional
    public Customer create(Customer customer) {
        // If the customer already exists, throw an exception
        if (repository.existsByIdentification(customer.getIdentification())) {
            throw new DuplicateIdentificationException(customer.getIdentification());
        }

        // If the customer status is null, set it to true
        if (customer.getStatus() == null) {
            customer.setStatus(true);
        }
        // Save the customer
        Customer saved = repository.save(customer);
        // Log the creation of the customer
        log.info("Customer created with id: {}", saved.getId());

        // Finally return the saved customer
        return saved;
    }

    // This method works to find a customer by id, if not found, it throws an
    // exception
    public Customer findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    // This methos works to find all customers
    public List<Customer> findAll() {
        return repository.findAll();
    }

    // This method works to delete a customer, if not found, it throws an exception
    public void delete(String id) {
        if (!repository.findById(id).isPresent()) {
            throw new CustomerNotFoundException(id);
        }
        repository.deleteById(id);
    }

    // Method for update a customer

    @Transactional
    public Customer update(String id, Customer customer) {
        // If the customer does not exist, throw an exception
        Customer existing = repository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        // Override the existing customer with the new values
        existing.setName(customer.getName());
        existing.setIdentification(customer.getIdentification());
        existing.setGender(customer.getGender());
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        existing.setStatus(customer.getStatus());

        // Save the updated customer
        Customer updated = repository.save(existing);
        log.info("Customer updated with id: {}", updated.getId());

        return updated;
    }

    @Transactional
    public Customer patch(String id, CustomerPatchDto patchDto) {
        // 1. Buscar cliente existente
        Customer existing = findById(id);

        // 2. Actualizar solo los campos que NO sean nulos en la petición
        if (patchDto.getName() != null) {
            existing.setName(patchDto.getName());
        }
        if (patchDto.getAddress() != null) {
            existing.setAddress(patchDto.getAddress());
        }
        if (patchDto.getPhone() != null) {
            existing.setPhone(patchDto.getPhone());
        }
        if (patchDto.getStatus() != null) {
            existing.setStatus(patchDto.getStatus());
        }

        // 3. Guardar cliente actualizado
        return repository.save(existing);
    }

}
