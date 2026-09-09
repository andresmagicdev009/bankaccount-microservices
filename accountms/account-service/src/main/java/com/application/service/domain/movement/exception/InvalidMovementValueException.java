package com.application.service.domain.movement.exception;

import java.math.BigDecimal;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.InvalidInputException;

/**
 * Rule F2: the movement value must be strictly greater than zero -> 400.
 *
 * Why 400 and not 422: it is rejected without looking at the state of the
 * account, so it is invalid input. The 422 is left for
 * InsufficientBalanceException alone.
 */
public class InvalidMovementValueException extends InvalidInputException {

    public InvalidMovementValueException(BigDecimal value) {
        super(ErrorCode.INVALID_MOVEMENT_VALUE, value);
    }
}
