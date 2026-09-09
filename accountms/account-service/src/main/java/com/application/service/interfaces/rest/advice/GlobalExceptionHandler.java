package com.application.service.interfaces.rest.advice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import com.application.service.domain.shared.exception.BusinessRuleException;
import com.application.service.domain.shared.exception.DomainException;
import com.application.service.domain.shared.exception.ExternalDependencyException;
import com.application.service.domain.shared.exception.InvalidInputException;
import com.application.service.domain.shared.exception.ResourceNotFoundException;
import com.application.service.domain.shared.exception.StateConflictException;
import com.application.service.interfaces.rest.dto.ErrorDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Translator from domain exceptions to the codes of the contract.
 *
 * This class is what lets the services stay ignorant of HTTP: they throw
 * business exceptions and here they are turned into the ErrorDto generated from
 * the YAML (components/schemas/Error).
 *
 * There is one handler per CATEGORY, not per concrete exception. Spring
 * dispatches polymorphically: @ExceptionHandler(ResourceNotFoundException.class)
 * also catches AccountNotFoundException, MovementNotFoundException and
 * CustomerNotFoundException. That is why adding a new exception does not force
 * a change here: it only has to hang off the right category.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** The single place where the error body is assembled. */
    private ResponseEntity<ErrorDto> build(HttpStatus status, String message, ServerWebExchange exchange) {
        ErrorDto body = new ErrorDto()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getPath().value());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * The 4xx responses are normal flow: somebody asked for something that does
     * not exist or sent bad data. They are logged as warn and without a stack
     * trace, so that ERROR in the log keeps meaning "something broke on our
     * side".
     */
    private void logClientError(HttpStatus status, DomainException ex, ServerWebExchange exchange) {
        log.warn("{} {} -> {} [{}] {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value(),
                status.value(),
                ex.getCode(),
                ex.getMessage());
    }

    // ---------------------------------------------------------------- 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(ResourceNotFoundException ex, ServerWebExchange exchange) {
        logClientError(HttpStatus.NOT_FOUND, ex, exchange);
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    // ---------------------------------------------------------------- 400
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorDto> handleInvalidInput(InvalidInputException ex, ServerWebExchange exchange) {
        logClientError(HttpStatus.BAD_REQUEST, ex, exchange);
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    /**
     * A 400 that does not come from the domain but from the framework:
     * malformed JSON, a UUID that does not parse in the path, or @Valid
     * rejecting the body against the contract (WebExchangeBindException extends
     * ServerWebInputException).
     */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorDto> handleMalformedRequest(ServerWebInputException ex, ServerWebExchange exchange) {
        String message = (ex instanceof WebExchangeBindException bindEx)
                ? bindEx.getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("Invalid request body")
                : "Malformed request";
        log.warn("{} {} -> 400 {}",
                exchange.getRequest().getMethod(), exchange.getRequest().getPath().value(), message);
        return build(HttpStatus.BAD_REQUEST, message, exchange);
    }

    // ---------------------------------------------------------------- 409
    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<ErrorDto> handleStateConflict(StateConflictException ex, ServerWebExchange exchange) {
        logClientError(HttpStatus.CONFLICT, ex, exchange);
        return build(HttpStatus.CONFLICT, ex.getMessage(), exchange);
    }

    // ---------------------------------------------------------------- 422
    /**
     * In Spring 7 the constant is UNPROCESSABLE_CONTENT; UNPROCESSABLE_ENTITY
     * was deprecated. The message leaves the exception untouched: for an
     * insufficient balance it is the literal "Saldo no disponible" required by
     * the specification.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorDto> handleBusinessRule(BusinessRuleException ex, ServerWebExchange exchange) {
        logClientError(HttpStatus.UNPROCESSABLE_CONTENT, ex, exchange);
        return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), exchange);
    }

    // ---------------------------------------------------------------- 502
    /**
     * Here a stack trace IS wanted: the customer microservice failed and the
     * cause (timeout, DNS, 5xx) is needed to diagnose it. Our own service is
     * fine, which is why this is a 502 and not a 500.
     */
    @ExceptionHandler(ExternalDependencyException.class)
    public ResponseEntity<ErrorDto> handleExternalDependency(ExternalDependencyException ex, ServerWebExchange exchange) {
        log.error("Upstream failure [{}]: {}", ex.getCode(), ex.getMessage(), ex);
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), exchange);
    }

    // ---------------------------------------------------------------- 500
    /**
     * Safety net. It logs the full stack trace, but NEVER returns
     * ex.getMessage() to the client: that text can leak table names, file paths
     * or SQL.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception on {} {}",
                exchange.getRequest().getMethod(), exchange.getRequest().getPath().value(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error", exchange);
    }
}
