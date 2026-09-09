package com.application.service.domain.account.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.StateConflictException;

/**
 * The account exists but its state does not accept movements -> 409.
 *
 * Same category as AccountBalanceNotZeroException and for the same reason: the
 * resource is there, it is its state that blocks the operation. It is not a
 * 404, which would lie to the client by saying the account does not exist, nor
 * a 422, which is reserved for rules about the request data (rule F3).
 */
public class InactiveAccountException extends StateConflictException {

    public InactiveAccountException(String accountNumber) {
        super(ErrorCode.ACCOUNT_INACTIVE, accountNumber);
    }
}
