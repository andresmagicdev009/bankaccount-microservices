package com.application.service.domain.account.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.StateConflictException;

/** An account holding a balance cannot be deleted -> 409. */
public class AccountBalanceNotZeroException extends StateConflictException {

    public AccountBalanceNotZeroException(String accountNumber) {
        super(ErrorCode.BALANCE_NOT_ZERO, accountNumber);
    }
}