package com.application.service.domain.shared.exception.StateConflictException;

import com.application.service.domain.shared.exception.DomainException;

/**
 * Categoria de dominio: la peticion es valida, pero el estado actual del
 * recurso impide la operacion. ConflictStrategy la traduce a 409.
 *
 * Diferencia con BusinessRuleViolationException (422): aqui el conflicto es de
 * estado del recurso (existe y esta como no debe); alla es una regla de negocio
 * que la operacion viola.
 */
public abstract class StateConflictException extends DomainException {

    protected StateConflictException(String code, String message) {
        super(code, message);
    }
}
