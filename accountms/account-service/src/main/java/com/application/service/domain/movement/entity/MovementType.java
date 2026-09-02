package com.application.service.domain.movement.entity;

/**
 * PASO 1.3 - Tipos de movimiento.
 *
 * TODO 1: declara DEBIT y CREDIT (mismos nombres que el contrato).
 *
 * TODO 2: agrega el metodo signed(BigDecimal value).
 *         Es la regla F2 del enunciado convertida en codigo:
 *           DEBIT  -> devuelve value.negate()  (resta del saldo)
 *           CREDIT -> devuelve value           (suma al saldo)
 *         Ponerlo aqui evita repetir if/else en el servicio.
 */
public enum MovementType {
    // TODO: DEBIT, CREDIT
    PENDIENTE
}
