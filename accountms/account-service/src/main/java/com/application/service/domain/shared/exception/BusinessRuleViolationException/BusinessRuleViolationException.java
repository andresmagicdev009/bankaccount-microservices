package com.application.service.domain.shared.exception.BusinessRuleViolationException;

import com.application.service.domain.shared.exception.DomainException;

/**
 * Categoria de dominio: la peticion esta bien formada y el recurso existe,
 * pero ejecutarla violaria una regla de negocio.
 * UnprocessableStrategy la traduce a 422.
 */
public abstract class BusinessRuleViolationException extends DomainException {

    protected BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
