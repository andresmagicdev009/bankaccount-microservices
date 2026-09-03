package com.application.service.domain.shared.exception;

import com.application.service.domain.shared.constant.ErrorCode;

/**
 * Categoria 502 - un servicio del que dependemos no respondio.
 *
 * Es abstracta a proposito: nadie debe lanzar la categoria directamente, sino
 * una subclase con nombre propio. El advice atrapa la categoria y cubre todas
 * sus subclases (presentes y futuras) con un solo @ExceptionHandler.
 */
public abstract class ExternalDependencyException extends DomainException {

    protected ExternalDependencyException(ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args));
    }

    /** El cause va primero: en Java los varargs tienen que cerrar la firma. */
    protected ExternalDependencyException(Throwable cause, ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args), cause);
    }
}
