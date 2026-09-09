package com.example.customerms.domain.shared.exception;

import lombok.Getter;

/**
 * Root of every business exception.
 *
 * It carries two things besides the message: the ErrorType (which status it
 * maps to) and a stable code for the logs, which does not change even if the
 * text is rewritten.
 *
 * It is abstract on purpose: nobody throws the root, always a subclass with a
 * name of its own. The advice catches DomainException and thereby covers all of
 * its subclasses -present and future- with a single @ExceptionHandler.
 */
@Getter
public abstract class DomainException extends RuntimeException {

    private final ErrorType type;
    private final String code;

    protected DomainException(ErrorType type, String code, String message) {
        super(message);
        this.type = type;
        this.code = code;
    }

    /**
     * For failures that wrap another one: keeps the cause so the stack trace
     * says what actually failed.
     */
    protected DomainException(ErrorType type, String code, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.code = code;
    }
}
