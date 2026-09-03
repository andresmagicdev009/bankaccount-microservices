package com.application.service.domain.shared.exception;

import com.application.service.domain.shared.constant.ErrorCode;

/**
 * Categoria 400 - la peticion viene mal formada o fuera de rango.
 *
 * Es abstracta a proposito: nadie debe lanzar la categoria directamente, sino
 * una subclase con nombre propio. El advice atrapa la categoria y cubre todas
 * sus subclases (presentes y futuras) con un solo @ExceptionHandler.
 */
public abstract class InvalidInputException extends DomainException {

    protected InvalidInputException(ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args));
    }

    /** El cause va primero: en Java los varargs tienen que cerrar la firma. */
    protected InvalidInputException(Throwable cause, ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args), cause);
    }
}
