package com.application.service.interfaces.rest.exception;

import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.customer.exception.CustomerNotFoundException;
import com.application.service.domain.movement.exception.MovementNotFoundException;
import com.application.service.interfaces.rest.dto.ErrorDto;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorDto> build(HttpStatus status, String message, ServerWebExchange exchange) {
        ErrorDto body = new ErrorDto()
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(exchange.getRequest().getPath().value());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler({AccountNotFoundException.class, MovementNotFoundException.class, CustomerNotFoundException.class})
    public ResponseEntity<ErrorDto> handleNotFound(RunTimeException ex, ServerWebExchange exchange) {
        log.error("Not found exception: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }
}
