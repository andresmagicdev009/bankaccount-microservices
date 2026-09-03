package com.application.service.domain.movement.exception;

import java.math.BigDecimal;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.InvalidInputException;

/**
 * PASO 1.13 - Regla F2: el valor del movimiento debe ser estrictamente mayor a
 * cero -> 400.
 *
 * Por que 400 y no 422: se rechaza sin mirar el estado de la cuenta, asi que es
 * entrada invalida. El 422 queda solo para InsufficientBalanceException.
 */
public class InvalidMovementValueException extends InvalidInputException {

    public InvalidMovementValueException(BigDecimal value) {
        super(ErrorCode.INVALID_MOVEMENT_VALUE, value);
    }
}
