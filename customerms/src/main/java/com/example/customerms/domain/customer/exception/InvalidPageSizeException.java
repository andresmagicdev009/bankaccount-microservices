package com.example.customerms.domain.customer.exception;

import com.example.customerms.domain.shared.exception.DomainException;
import com.example.customerms.domain.shared.exception.ErrorType;

/**
 * No @ResponseStatus any more: the status comes from the ErrorType and is
 * applied by the advice, which also returns the Error body of the contract.
 */
public class InvalidPageSizeException extends DomainException {

    public InvalidPageSizeException(Integer size, String message) {
        super(ErrorType.INVALID_INPUT, "INVALID_PAGE_SIZE",
                message + " Invalid page size: " + size + ". Page size must be between 1 and 100.");
    }
}
