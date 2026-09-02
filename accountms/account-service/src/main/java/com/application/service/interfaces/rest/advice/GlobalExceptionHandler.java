package com.application.service.interfaces.rest.advice;

/**
 * PASO 8 - Traductor de excepciones de dominio a los codigos del contrato.
 *
 * Esta clase es la que permite que los servicios no sepan de HTTP: ellos lanzan
 * excepciones de negocio, y aqui se convierten en el ErrorDto generado del YAML
 * (components/schemas/Error).
 *
 * TODO 0: @RestControllerAdvice @Slf4j
 *
 * TODO 1: metodo privado build(HttpStatus status, String message, ServerWebExchange exchange)
 *         que arme el ErrorDto con timestamp (OffsetDateTime.now(ZoneOffset.UTC)),
 *         status, error (status.getReasonPhrase()), message y
 *         path (exchange.getRequest().getPath().value()).
 *         Escribelo primero: los demas handlers son una linea cada uno.
 *
 * TODO 2: un @ExceptionHandler por familia
 *         404 -> AccountNotFoundException, MovementNotFoundException, CustomerNotFoundException
 *         400 -> InvalidMovementValueException, InvalidPageSizeException,
 *                InvalidDateRangeException, ServerWebInputException,
 *                WebExchangeBindException (esta ultima es la que lanza @Valid
 *                cuando el body no cumple el contrato)
 *         409 -> AccountBalanceNotZeroException
 *         422 -> InsufficientBalanceException, con el mensaje EXACTO
 *                "Saldo no disponible" (usa la constante MESSAGE).
 *                Nota: en Spring 7 la constante es HttpStatus.UNPROCESSABLE_CONTENT;
 *                UNPROCESSABLE_ENTITY quedo deprecada.
 *         502 -> CustomerServiceUnavailableException
 *         500 -> Exception generica, y aqui SI loguea el stacktrace completo.
 *                No devuelvas el mensaje interno al cliente.
 */
public class GlobalExceptionHandler {

}
