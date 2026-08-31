package com.example.customerms.application.customer.mapper;

import com.example.customerms.application.customer.dto.CustomerRequest;
import com.example.customerms.application.customer.dto.CustomerResponse;
import com.example.customerms.domain.customer.entity.Customer;

import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toDomain(CustomerRequest request) {
        return Customer.builder()
                .name(request.getName())
                .gender(request.getGender())
                .identification(request.getIdentification())
                .address(request.getAddress())
                .phone(request.getPhone())
                .password(request.getPassword())
                .status(request.getStatus())
                .build();
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getGender(),
                customer.getIdentification(),
                customer.getAddress(),
                customer.getPhone(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
