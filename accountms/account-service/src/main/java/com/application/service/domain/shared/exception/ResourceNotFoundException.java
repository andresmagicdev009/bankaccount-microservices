package com.application.service.domain.shared.exception;

import com.application.service.domain.shared.constant.ErrorCode;

/**
 * Categoria 404 - el recurso pedido no existe.
 *
 * Es abstracta a proposito: nadie debe lanzar la categoria directamente, sino
 * una subclase con nombre propio. El advice atrapa la categoria y cubre todas
 * sus subclases (presentes y futuras) con un solo @ExceptionHandler.
 */
public abstract class ResourceNotFoundException extends DomainException {

    protected ResourceNotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args));
    }

    /** El cause va primero: en Java los varargs tienen que cerrar la firma. */
    protected ResourceNotFoundException(Throwable cause, ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args), cause);
    }
}
