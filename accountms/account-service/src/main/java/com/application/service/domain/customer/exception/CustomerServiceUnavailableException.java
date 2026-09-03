package com.application.service.domain.customer.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.ExternalDependencyException;

/**
 * PASO 1.15 - El microservicio de clientes no respondio (timeout, conexion
 * rechazada, 5xx) -> 502, NO 500. La falla es de arriba, no nuestra.
 *
 * El cause es obligatorio: sin el pierdes el stacktrace del WebClient y al
 * depurar no distingues un timeout de un DNS caido.
 */
public class CustomerServiceUnavailableException extends ExternalDependencyException {

    public CustomerServiceUnavailableException(String customerId, Throwable cause) {
        super(cause, ErrorCode.CUSTOMER_SERVICE_UNAVAILABLE, customerId);
    }
}
