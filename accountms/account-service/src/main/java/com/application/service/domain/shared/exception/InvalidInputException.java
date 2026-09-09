package com.application.service.domain.shared.exception;

import com.application.service.domain.shared.constant.ErrorCode;

/**
 * Category 400 - the request is malformed or out of range.
 *
 * It is abstract on purpose: nobody should throw the category directly, always
 * a subclass with a name of its own. The advice catches the category and covers
 * all of its subclasses (present and future) with a single @ExceptionHandler.
 */
public abstract class InvalidInputException extends DomainException {

    protected InvalidInputException(ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args));
    }

    /** The cause comes first: in Java the varargs have to close the signature. */
    protected InvalidInputException(Throwable cause, ErrorCode errorCode, Object... args) {
        super(errorCode.code(), errorCode.format(args), cause);
    }
}
