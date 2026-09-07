package com.example.customerms.domain.customer.exception;

import com.example.customerms.domain.shared.exception.DomainException;
import com.example.customerms.domain.shared.exception.ErrorType;

public class CustomerNotFoundException extends DomainException {

    public CustomerNotFoundException(String id) {
        super(ErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found with id: " + id);
    }
}
