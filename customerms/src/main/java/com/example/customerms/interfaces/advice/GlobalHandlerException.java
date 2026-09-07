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
import org.springframework.web.server.ServerWebInputException;

import com.example.customerms.domain.shared.exception.DomainException;
import com.example.customerms.domain.shared.exception.ErrorType;
import com.example.customerms.interfaces.rest.dto.ErrorDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Traductor de excepciones de dominio a los codigos del contrato.
 *
 * Gracias a esta clase el servicio no sabe de HTTP: lanza excepciones de
 * negocio y aqui se convierten en el ErrorDto generado del YAML
 * (components/schemas/Error), que es el cuerpo que declaran BadRequest,
 * NotFound, Conflict e InternalError.
 *
 * Hay UN handler para todo el dominio. Spring despacha por polimorfismo, asi
 * que @ExceptionHandler(DomainException.class) atrapa a CustomerNotFound,
 * DuplicateIdentification, InvalidaPageSize y a las que vengan: una excepcion
 * nueva no obliga a tocar este archivo, basta con que elija su ErrorType.
 *
 * Al ser @RestControllerAdvice tambien cubre a los controllers reactivos:
 * WebFlux propaga la excepcion por el Mono y la despacha aqui, asi que sirve
 * igual aunque el trabajo bloqueante corra en el scheduler jdbc.
 */
@RestControllerAdvice
@Slf4j
public class GlobalHandlerException {

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

    /** Unico sitio donde el dominio se cruza con HTTP. */
    private HttpStatus toStatus(ErrorType type) {
        return switch (type) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
    }

    // ------------------------------------------------- dominio: 404/400/409
    /**
     * Los 4xx son flujo normal: alguien pidio algo que no existe o mando datos
     * malos. Se loguean como warn y sin stacktrace, para que un ERROR en el log
     * siga significando "algo se rompio de nuestro lado".
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
    /**
     * El duplicado visto desde la base: el chequeo previo de CustomerService
     * pierde ante dos altas simultaneas y salta la unique uq_person_identification.
     * Se responde 409 igual, pero sin devolver el mensaje de la BD (filtraria
     * nombres de tablas e indices).
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
