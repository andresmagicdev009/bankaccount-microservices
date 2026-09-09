package com.application.service.domain.shared.exception;


public abstract class DomainException extends RuntimeException {
    
    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * For failures originated in another system: keeps the cause so the stack
     * trace of the 502 says what actually failed.
     */
    protected DomainException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
