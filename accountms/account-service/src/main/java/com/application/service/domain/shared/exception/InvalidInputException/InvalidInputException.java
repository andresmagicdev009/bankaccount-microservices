package com.application.service.domain.shared.exception.InvalidInputException;

import com.application.service.domain.shared.exception.DomainException;

/**
 * Categoria de dominio: la peticion esta mal formada o fuera de rango.
 * BadRequestStrategy la traduce a 400.
 *
 * Se rechaza SIN mirar el estado del sistema: no se consulta la BD, no importa
 * el saldo. Si la peticion es correcta pero choca contra el estado, no es esta
 * categoria: eso es BusinessRuleViolationException (422) o StateConflictException (409).
 */
public abstract class InvalidInputException extends DomainException {

    protected InvalidInputException(String code, String message) {
        super(code, message);
    }
}
