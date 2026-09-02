package com.application.service.domain.movement.exception;

/**
 * PASO 1.12 - Regla de negocio F3: saldo insuficiente -> 422.
 *
 * OJO: el enunciado exige que el mensaje sea EXACTAMENTE "Saldo no disponible".
 * No lo traduzcas ni le agregues detalles.
 *
 * TODO 1: extiende RuntimeException.
 * TODO 2: declara public static final String MESSAGE = "Saldo no disponible";
 * TODO 3: constructor sin argumentos que haga super(MESSAGE).
 *         Tener la constante evita que el texto se escriba a mano en el advice.
 */
public class InsufficientBalanceException {

}
