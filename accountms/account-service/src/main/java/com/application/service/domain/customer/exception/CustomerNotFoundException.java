package com.application.service.domain.customer.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.ResourceNotFoundException;

/** PASO 1.14 - El microservicio de clientes respondio 404 para ese id -> 404 aqui. */
public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(String customerId) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, "customerId", customerId);
    }
}
