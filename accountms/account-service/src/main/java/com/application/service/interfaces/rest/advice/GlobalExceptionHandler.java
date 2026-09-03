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
 * PASO 8 - Traductor de excepciones de dominio a los codigos del contrato.
 *
 * Esta clase es la que permite que los servicios no sepan de HTTP: ellos lanzan
 * excepciones de negocio y aqui se convierten en el ErrorDto generado del YAML
 * (components/schemas/Error).
 *
 * Hay un handler por CATEGORIA, no por excepcion concreta. Spring despacha por
 * polimorfismo: @ExceptionHandler(ResourceNotFoundException.class) atrapa
 * tambien a AccountNotFoundException, MovementNotFoundException y
 * CustomerNotFoundException. Por eso agregar una excepcion nueva no obliga a
 * tocar este archivo: basta con colgarla de la categoria correcta.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Unico sitio donde se arma el cuerpo del error. */
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
     * Los 4xx son flujo normal: alguien pidio algo que no existe o mando datos
     * malos. Se loguean como warn y sin stacktrace, para que el ERROR del log
     * siga significando "algo se rompio de nuestro lado".
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
     * 400 que no nace del dominio sino del framework: JSON mal formado, un UUID
     * que no parsea en el path, o @Valid rechazando el body contra el contrato
     * (WebExchangeBindException hereda de ServerWebInputException).
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
     * En Spring 7 la constante es UNPROCESSABLE_CONTENT; UNPROCESSABLE_ENTITY
     * quedo deprecada. El mensaje sale intacto de la excepcion: para saldo
     * insuficiente es el literal "Saldo no disponible" que exige el enunciado.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorDto> handleBusinessRule(BusinessRuleException ex, ServerWebExchange exchange) {
        logClientError(HttpStatus.UNPROCESSABLE_CONTENT, ex, exchange);
        return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), exchange);
    }

    // ---------------------------------------------------------------- 502
    /**
     * Aqui SI va stacktrace: el microservicio de clientes fallo y necesitamos la
     * causa (timeout, DNS, 5xx) para diagnosticar. El servicio propio esta bien,
     * por eso es 502 y no 500.
     */
    @ExceptionHandler(ExternalDependencyException.class)
    public ResponseEntity<ErrorDto> handleExternalDependency(ExternalDependencyException ex, ServerWebExchange exchange) {
        log.error("Upstream failure [{}]: {}", ex.getCode(), ex.getMessage(), ex);
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), exchange);
    }

    // ---------------------------------------------------------------- 500
    /**
     * Red de seguridad. Loguea el stacktrace completo, pero NUNCA devuelve
     * ex.getMessage() al cliente: ese texto puede filtrar nombres de tablas,
     * rutas de archivos o SQL.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception on {} {}",
                exchange.getRequest().getMethod(), exchange.getRequest().getPath().value(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error", exchange);
    }
}
