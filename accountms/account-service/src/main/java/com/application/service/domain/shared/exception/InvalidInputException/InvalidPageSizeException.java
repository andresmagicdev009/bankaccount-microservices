package com.application.service.domain.shared.exception.InvalidInputException;

/**
 * Tamano de pagina fuera del rango declarado en el contrato
 * (components/parameters/SizeParam: minimum 1, maximum 100).
 */
public class InvalidPageSizeException extends InvalidInputException {

    public InvalidPageSizeException(int size) {
        super("INVALID_PAGE_SIZE", "Page size must be between 1 and 100, got: " + size);
    }
}
