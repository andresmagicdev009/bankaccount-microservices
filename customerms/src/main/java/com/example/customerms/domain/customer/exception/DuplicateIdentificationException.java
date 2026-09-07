package com.example.customerms.domain.customer.exception;

import com.example.customerms.domain.shared.exception.DomainException;
import com.example.customerms.domain.shared.exception.ErrorType;

public class DuplicateIdentificationException extends DomainException {

    public DuplicateIdentificationException(String identification) {
        super(ErrorType.CONFLICT, "DUPLICATE_IDENTIFICATION",
                "Customer already exists with identification: " + identification);
    }
}
