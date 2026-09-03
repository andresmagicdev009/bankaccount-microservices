package com.application.service.domain.account.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.ResourceNotFoundException;

/** PASO 1.9 - Cuenta inexistente -> 404. */
public class AccountNotFoundException extends ResourceNotFoundException {

    public AccountNotFoundException(String accountNumber) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, "accountNumber", accountNumber);
    }
}
