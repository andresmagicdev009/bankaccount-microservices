package com.application.service.domain.shared.exception.ExternalDependencyException;

import com.application.service.domain.shared.exception.DomainException;

/**
 * Categoria de dominio: un sistema del que dependemos no respondio o respondio
 * con error. BadGatewayStrategy la traduce a 502, NO a 500: la falla es de
 * arriba, no nuestra.
 *
 * Siempre con la causa: sin ella el log del 502 no dice si fue timeout,
 * conexion rechazada o un 5xx del otro lado.
 */
public abstract class ExternalDependencyException extends DomainException {

    protected ExternalDependencyException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
