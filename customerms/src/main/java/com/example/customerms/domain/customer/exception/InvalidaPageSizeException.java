package com.example.customerms.domain.customer.exception;

import com.example.customerms.domain.shared.exception.DomainException;
import com.example.customerms.domain.shared.exception.ErrorType;

/**
 * Ya no lleva @ResponseStatus: el status sale del ErrorType y lo aplica el
 * advice, que ademas devuelve el cuerpo Error del contrato.
 */
public class InvalidaPageSizeException extends DomainException {

    public InvalidaPageSizeException(Integer size, String message) {
        super(ErrorType.INVALID_INPUT, "INVALID_PAGE_SIZE",
                message + " Invalid page size: " + size + ". Page size must be between 1 and 100.");
    }
}
