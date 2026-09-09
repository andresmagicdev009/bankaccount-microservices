package com.application.service.domain.customer.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.ExternalDependencyException;

/**
 * The customer microservice did not answer (timeout, refused connection, 5xx)
 * -> 502, NOT 500. The failure is upstream, not ours.
 *
 * The cause is mandatory: without it the WebClient stack trace is lost and,
 * while debugging, a timeout cannot be told apart from a dead DNS.
 */
public class CustomerServiceUnavailableException extends ExternalDependencyException {

    public CustomerServiceUnavailableException(String customerId, Throwable cause) {
        super(cause, ErrorCode.CUSTOMER_SERVICE_UNAVAILABLE, customerId);
    }
}
