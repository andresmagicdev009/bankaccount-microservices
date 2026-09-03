package com.application.service.interfaces.rest.advice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import com.application.service.domain.shared.exception.DomainException;
import com.application.service.interfaces.rest.advice.strategy.ExceptionStrategy;
import com.application.service.interfaces.rest.dto.ErrorDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PASO 8 - Traductor de excepciones de dominio a los codigos del contrato.
 *
 * Esta clase es la que permite que los servicios no sepan de HTTP: ellos lanzan
 * excepciones de negocio, y aqui se convierten en el ErrorDto generado del YAML
 * (components/schemas/Error).
 *
 * No contiene ningun mapeo excepcion -> status. Ese conocimiento vive en las
 * ExceptionStrategy, una por categoria de dominio. Por eso esta clase no vuelve
 * a cambiar cuando aparece una excepcion nueva.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE = "Error interno del servidor";

    /**
     * Spring inyecta TODAS las implementaciones de ExceptionStrategy que
     * encuentre como bean. Aqui esta el OCP: agregar una categoria nueva es
     * agregar un @Component, no editar este archivo.
     */
    private final List<ExceptionStrategy> strategies;

    /**
     * Toda excepcion de dominio pasa por aqui. La strategy decide el status;
     * el mensaje sale de la propia excepcion porque ya fue escrito para el
     * cliente (ver InsufficientBalanceException.MESSAGE).
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorDto> handleDomain(DomainException ex, ServerWebExchange exchange) {
        HttpStatus status = resolveStatus(ex);

        log.warn("{} [{}] -> {} {}", ex.getClass().getSimpleName(), ex.getCode(),
                status.value(), ex.getMessage());

        return ResponseEntity.status(status).body(build(status, ex.getMessage(), exchange));
    }

    /**
     * Red de seguridad: lo que no es del dominio es un bug o una falla de
     * infraestructura. Unico sitio donde se loguea el stacktrace completo, y el
     * unico donde el mensaje al cliente NO viene de la excepcion: nunca se
     * filtra el detalle interno.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Error no controlado en {}", exchange.getRequest().getPath().value(), ex);
        return ResponseEntity.status(status).body(build(status, INTERNAL_ERROR_MESSAGE, exchange));
    }

    /**
     * isInstance, no equals: una strategy registrada sobre la categoria cubre a
     * todas sus subclases sin tener que enumerarlas.
     *
     * El orElse es defensivo. Si salta, falta la strategy de esa categoria.
     */
    private HttpStatus resolveStatus(DomainException ex) {
        return strategies.stream()
                .filter(strategy -> strategy.handles().isInstance(ex))
                .findFirst()
                .map(ExceptionStrategy::status)
                .orElseGet(() -> {
                    log.error("Sin ExceptionStrategy para {}", ex.getClass().getName());
                    return HttpStatus.INTERNAL_SERVER_ERROR;
                });
    }

    private ErrorDto build(HttpStatus status, String message, ServerWebExchange exchange) {
        return new ErrorDto()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(Integer.valueOf(status.value()))
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getPath().value());
    }
}
