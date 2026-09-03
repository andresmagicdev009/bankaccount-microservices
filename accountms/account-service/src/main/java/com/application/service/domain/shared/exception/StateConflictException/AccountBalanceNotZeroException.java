package com.application.service.domain.shared.exception.StateConflictException;

import java.math.BigDecimal;

/**
 * No se puede borrar una cuenta con saldo. DELETE /accounts/{accountNumber}
 * declara '409': The account cannot be deleted because its balance is not zero.
 */
public class AccountBalanceNotZeroException extends StateConflictException {

    public AccountBalanceNotZeroException(String accountNumber, BigDecimal balance) {
        super("ACCOUNT_BALANCE_NOT_ZERO",
                "Account " + accountNumber + " cannot be deleted, balance is not zero: " + balance);
    }
}
