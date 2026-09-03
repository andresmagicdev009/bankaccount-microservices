package com.application.service.domain.shared.exception.InvalidInputException;

import java.math.BigDecimal;

/**
 * Regla F2: el valor del movimiento debe ser estrictamente mayor a cero.
 *
 * Por que 400 y no 422: lo decide el contrato. POST /movements declara
 * '400': Invalid request (e.g. value is not greater than zero). Suena a regla
 * de negocio, pero se rechaza sin mirar el estado de la cuenta.
 */
public class InvalidMovementValueException extends InvalidInputException {

    public InvalidMovementValueException(BigDecimal value) {
        super("INVALID_MOVEMENT_VALUE", "Movement value must be greater than zero, got: " + value);
    }
}
