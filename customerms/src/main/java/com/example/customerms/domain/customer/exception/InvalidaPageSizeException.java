package com.example.customerms.domain.customer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // Fuerza el codigo HTTP 400
public class InvalidaPageSizeException extends RuntimeException {

    public InvalidaPageSizeException(Integer size, String message) {
        super(message + " Invalid page size: " + size + ". Page size must be between 1 and 100.");
    }
    
}
