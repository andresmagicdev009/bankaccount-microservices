package com.example.customerms.application.customer.service;

import com.example.customerms.domain.customer.entity.Customer;
import com.example.customerms.domain.customer.exception.CustomerNotFoundException;
import com.example.customerms.domain.customer.exception.DuplicateIdentificationException;
import com.example.customerms.domain.customer.exception.InvalidPageSizeException;
import com.example.customerms.domain.customer.repository.CustomerRepositoryPort;
import com.example.customerms.interfaces.rest.dto.CustomerPatchDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // This method works to find all customers
    public Page<Customer> findAll(Integer page, Integer size, Boolean status) {
        int pageNumber = (page == null || page < 0) ? 0: page;

        // Use an exception to validate the page size.
        if (size != null && (size < 1 || size > 100)) {
            throw new InvalidPageSizeException(size, "Invalid page size.");
        }
        int pageSize = (size == null) ? 20: size;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return repository.findAll(status, pageable);
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
        // PUT is a full replacement and the contract declares password as
        // required (@NotNull on CustomerUpdateDto), so it always arrives: it is
        // applied like every other field.
        existing.setPassword(customer.getPassword());

        // Save the updated customer
        Customer updated = repository.save(existing);
        log.info("Customer updated with id: {}", updated.getId());

        return updated;
    }

    @Transactional
    public Customer patch(String id, CustomerPatchDto patchDto) {
        // 1. Look up the existing customer
        Customer existing = findById(id);

        // 2. Update only the fields that are NOT null in the request
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
        // password is patchable too, per the contract. It gets a null guard like
        // the rest: in a PATCH, an absent field means "leave it as it is".
        if (patchDto.getPassword() != null) {
            existing.setPassword(patchDto.getPassword());
        }

        // 3. Save the updated customer
        return repository.save(existing);
    }

}
