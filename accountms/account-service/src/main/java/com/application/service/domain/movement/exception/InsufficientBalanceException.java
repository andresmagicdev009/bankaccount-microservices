package com.application.service.domain.movement.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.BusinessRuleException;

/**
 * Business rule F3: insufficient balance -> 422.
 *
 * The message reaching the client is EXACTLY "Saldo no disponible": it is
 * defined by ErrorCode.INSUFFICIENT_BALANCE and takes no arguments. The detail
 * needed for debugging (available vs. requested balance) goes into the
 * MovementService log, never into the response body.
 */
public class InsufficientBalanceException extends BusinessRuleException {

    public InsufficientBalanceException() {
        super(ErrorCode.INSUFFICIENT_BALANCE);
    }
}
