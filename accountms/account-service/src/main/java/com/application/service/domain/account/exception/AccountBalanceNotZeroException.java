package com.application.service.domain.account.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.StateConflictException;

/** PASO 1.10 - No se puede borrar una cuenta con saldo -> 409. */
public class AccountBalanceNotZeroException extends StateConflictException {

    public AccountBalanceNotZeroException(String accountNumber) {
        super(ErrorCode.BALANCE_NOT_ZERO, accountNumber);
    }
}