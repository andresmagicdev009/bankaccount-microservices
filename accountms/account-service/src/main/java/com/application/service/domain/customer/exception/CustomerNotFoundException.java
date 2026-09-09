package com.application.service.domain.customer.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.ResourceNotFoundException;

/** The customer microservice answered 404 for that id -> 404 here as well. */
public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(String customerId) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, "customerId", customerId);
    }
}
