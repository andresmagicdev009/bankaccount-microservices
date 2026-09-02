package com.application.service.domain.customer.exception;

/**
 * PASO 1.15 - El microservicio de clientes no respondio (timeout, conexion
 * rechazada, 5xx) -> 502, NO 500. La falla es de arriba, no nuestra.
 *
 * TODO: extiende RuntimeException con constructor (String customerId, Throwable cause)
 *       y pasa la causa con super(mensaje, cause) para no perder el stacktrace.
 */
public class CustomerServiceUnavailableException {

}
