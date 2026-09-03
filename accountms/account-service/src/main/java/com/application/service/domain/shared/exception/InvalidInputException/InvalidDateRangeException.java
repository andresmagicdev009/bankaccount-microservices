package com.application.service.domain.shared.exception.InvalidInputException;

import java.time.LocalDate;

/**
 * startDate posterior a endDate. GET /reports/{client-id} declara
 * '400': Invalid date range (startDate after endDate, malformed date, etc.)
 *
 * La comparacion no vive aqui: la excepcion solo reporta, quien decide es el
 * servicio que valida el rango.
 */
public class InvalidDateRangeException extends InvalidInputException {

    public InvalidDateRangeException(LocalDate startDate, LocalDate endDate) {
        super("INVALID_DATE_RANGE",
                "startDate " + startDate + " must not be after endDate " + endDate);
    }
}
