package com.example.customerms.domain.customer.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String id) {
        super("Customer not found with id: " + id);
    }
}
