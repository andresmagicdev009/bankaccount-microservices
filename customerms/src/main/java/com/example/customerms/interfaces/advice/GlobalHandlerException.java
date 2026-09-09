package com.example.customerms.interfaces.advice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import com.example.customerms.domain.shared.exception.DomainException;
import com.example.customerms.domain.shared.exception.ErrorType;
import com.example.customerms.interfaces.rest.dto.ErrorDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Translator from domain exceptions to the codes of the contract.
 *
 * Thanks to this class the service knows nothing about HTTP: it throws business
 * exceptions and they are turned here into the ErrorDto generated from the YAML
 * (components/schemas/Error), which is the body declared by BadRequest,
 * NotFound, Conflict and InternalError.
 *
 * There is ONE handler for the whole domain. Spring dispatches polymorphically,
 * so @ExceptionHandler(DomainException.class) catches CustomerNotFound,
 * DuplicateIdentification, InvalidPageSize and whatever comes next: a new
 * exception does not force a change here, it only has to pick its ErrorType.
 *
 * Being a @RestControllerAdvice it also covers the reactive controllers:
 * WebFlux propagates the exception through the Mono and dispatches it here, so
 * it works just the same even though the blocking work runs on the jdbc
 * scheduler.
 */
@RestControllerAdvice
@Slf4j
public class GlobalHandlerException {

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

    /** The single place where the domain meets HTTP. */
    private HttpStatus toStatus(ErrorType type) {
        return switch (type) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
    }

    // -------------------------------------------------- domain: 404/400/409
    /**
     * The 4xx responses are normal flow: somebody asked for something that does
     * not exist or sent bad data. They are logged as warn and without a stack
     * trace, so that an ERROR in the log keeps meaning "something broke on our
     * side".
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorDto> handleDomain(DomainException ex, ServerWebExchange exchange) {
        HttpStatus status = toStatus(ex.getType());
        log.warn("{} {} -> {} [{}] {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value(),
                status.value(),
                ex.getCode(),
                ex.getMessage());
        return build(status, ex.getMessage(), exchange);
    }

    // ---------------------------------------------------------------- 400
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
    /**
     * The duplicate as seen from the database: the up-front check in
     * CustomerService loses against two simultaneous inserts and the
     * uq_person_identification unique constraint fires. The answer is still a
     * 409, but without returning the database message (it would leak table and
     * index names).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDto> handleDataIntegrity(DataIntegrityViolationException ex,
            ServerWebExchange exchange) {
        log.warn("{} {} -> 409 constraint violation: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value(),
                ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Customer already exists with that identification", exchange);
    }

    // --------------------------------------------------- framework statuses
    /**
     * Exceptions that already carry their own HTTP code: a non-existent route
     * (NoResourceFoundException -> 404), a method that is not allowed (405), an
     * Accept header that does not match (406). Without this handler they fell
     * into the safety net below and went out as 500, which lies to the client.
     *
     * ServerWebInputException also extends ResponseStatusException, but Spring
     * dispatches to the most specific handler, so the 400 above still wins.
     *
     * The answer carries the status reason phrase and not ex.getReason(): that
     * text includes the requested path and internal framework details.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorDto> handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        log.warn("{} {} -> {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value(),
                status.value(),
                ex.getReason());
        return build(status, status.getReasonPhrase(), exchange);
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
