package com.application.service.domain.shared.exception.BusinessRuleViolationException;

/**
 * Regla F3: saldo insuficiente. POST /movements declara
 * '422': Insufficient balance to perform the movement.
 *
 * OJO: el enunciado exige que el mensaje sea EXACTAMENTE "Saldo no disponible".
 * La constante existe para que ese texto no se reescriba a mano en el advice
 * ni en los tests.
 */
public class InsufficientBalanceException extends BusinessRuleViolationException {

    public static final String MESSAGE = "Saldo no disponible";

    public InsufficientBalanceException() {
        super("INSUFFICIENT_BALANCE", MESSAGE);
    }
}
