package com.example.customerms.domain.shared.exception;

import lombok.Getter;

/**
 * Raiz de todas las excepciones de negocio.
 *
 * Lleva dos datos ademas del mensaje: el ErrorType (que status le toca) y un
 * code estable para los logs, que no cambia aunque se reescriba el texto.
 *
 * Es abstracta a proposito: nadie lanza la raiz, siempre una subclase con
 * nombre propio. El advice atrapa DomainException y con eso cubre a todas sus
 * subclases -presentes y futuras- con un unico @ExceptionHandler.
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
     * Para fallas que envuelven a otra: conserva la causa para que el stacktrace
     * diga que fallo realmente.
     */
    protected DomainException(ErrorType type, String code, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.code = code;
    }
}
