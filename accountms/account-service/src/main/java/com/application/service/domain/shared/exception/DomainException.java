package com.application.service.domain.shared.exception;


public abstract class DomainException extends RuntimeException {
    
    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Para fallas originadas en otro sistema: conserva la causa para que el
     * stacktrace del 502 diga que fallo realmente.
     */
    protected DomainException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
