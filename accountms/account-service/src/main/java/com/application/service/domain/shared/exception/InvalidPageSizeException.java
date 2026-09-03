package com.application.service.domain.shared.exception;

import com.application.service.domain.shared.constant.ErrorCode;

/** PASO 1.16 - Tamano de pagina fuera del rango 1..100 -> 400. */
public class InvalidPageSizeException extends InvalidInputException {

    public InvalidPageSizeException(int size) {
        super(ErrorCode.INVALID_PAGE_SIZE, size);
    }
}
