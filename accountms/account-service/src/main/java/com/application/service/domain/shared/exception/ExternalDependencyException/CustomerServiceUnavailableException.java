package com.application.service.domain.shared.exception.ExternalDependencyException;

/**
 * El microservicio de clientes no respondio (timeout, conexion rechazada, 5xx).
 * El contrato lo declara como components/responses/UpstreamError -> 502.
 *
 * No confundir con CustomerNotFoundException: alla el MS respondio 404 (el
 * cliente no existe); aca el MS no pudo responder.
 */
public class CustomerServiceUnavailableException extends ExternalDependencyException {

    public CustomerServiceUnavailableException(String customerId, Throwable cause) {
        super("CUSTOMER_SERVICE_UNAVAILABLE",
                "Customer service did not respond for customer id: " + customerId, cause);
    }
}
