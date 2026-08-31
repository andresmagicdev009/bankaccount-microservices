package com.example.customerms.infrastructure.persistence.jpa.mapper;

import com.example.customerms.domain.customer.entity.Customer;
import com.example.customerms.infrastructure.persistence.jpa.entity.CustomerEntity;

import org.springframework.stereotype.Component;

@Component
public class CustomerPersistenceMapper {

    public CustomerEntity toEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.getId());
        copyState(customer, entity);
        return entity;
    }

    /** Copies mutable state onto a managed entity, keeping id and audit columns untouched. */
    public void updateEntity(CustomerEntity entity, Customer customer) {
        copyState(customer, entity);
    }

    public Customer toDomain(CustomerEntity entity) {
        return Customer.builder()
                .id(entity.getId())
                .name(entity.getName())
                .gender(entity.getGender())
                .identification(entity.getIdentification())
                .address(entity.getAddress())
                .phone(entity.getPhone())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .password(entity.getPassword())
                .status(entity.getStatus())
                .build();
    }

    private void copyState(Customer customer, CustomerEntity entity) {
        entity.setName(customer.getName());
        entity.setGender(customer.getGender());
        entity.setIdentification(customer.getIdentification());
        entity.setAddress(customer.getAddress());
        entity.setPhone(customer.getPhone());
        entity.setPassword(customer.getPassword());
        if (customer.getStatus() != null) {
            entity.setStatus(customer.getStatus());
        }
    }
}
