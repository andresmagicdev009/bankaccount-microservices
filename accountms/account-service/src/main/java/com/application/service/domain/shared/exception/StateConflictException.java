package com.application.service.domain.shared.exception;

import com.application.service.domain.shared.constant.ErrorCode;

/**
 * Categoria 409 - el recurso existe pero su estado no permite la operacion.
 *
 * Es abstracta a proposito: nadie debe lanzar la categoria directamente, sino
 * una subclase con nombre propio. El advice atrapa la categoria y cubre todas
 * sus subclases (presentes y futuras) con un solo @ExceptionHandler.
 */
public abstract class StateConflictException extends DomainException {

    protected StateConflictException(ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args));
    }

    /** El cause va primero: en Java los varargs tienen que cerrar la firma. */
    protected StateConflictException(Throwable cause, ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args), cause);
    }
}
