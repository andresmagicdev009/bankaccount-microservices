package com.application.service.domain.account.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.StateConflictException;

/**
 * La cuenta existe pero su estado no admite movimientos -> 409.
 *
 * Misma categoria que AccountBalanceNotZeroException y por la misma razon: el
 * recurso esta ahi, es su estado el que bloquea la operacion. No es un 404,
 * que le mentiria al cliente diciendole que la cuenta no existe, ni un 422,
 * que reservamos para reglas sobre los datos de la peticion (regla F3).
 */
public class InactiveAccountException extends StateConflictException {

    public InactiveAccountException(String accountNumber) {
        super(ErrorCode.ACCOUNT_INACTIVE, accountNumber);
    }
}
