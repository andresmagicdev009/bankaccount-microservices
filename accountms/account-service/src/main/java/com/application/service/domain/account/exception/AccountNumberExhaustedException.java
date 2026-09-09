package com.application.service.domain.account.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.DomainException;

/**
 * The domain ran out of account numbers.
 *
 * It hangs off DomainException and not off any of the four categories on
 * purpose: it is not the fault of the client -there is nothing they could fix
 * in their request-, it is exhausted system capacity. Since it fits none of
 * 400/404/409/422, GlobalExceptionHandler picks it up with
 * @ExceptionHandler(Exception.class) and it goes out as a 500 carrying the
 * generic text, which is exactly what is wanted: the detail stays in the log,
 * not in the response.
 */
public class AccountNumberExhaustedException extends DomainException {

    public AccountNumberExhaustedException() {
        super(ErrorCode.ACCOUNT_NUMBER_EXHAUSTED.code(),
                ErrorCode.ACCOUNT_NUMBER_EXHAUSTED.format());
    }
}
