package com.application.service.domain.movement.exception;

/**
 * PASO 1.13 - Regla F2: el valor del movimiento debe ser estrictamente mayor a cero -> 400.
 *
 * Nota: la validacion tambien viaja en el contrato (@DecimalMin inclusive=false
 * en MovementCreateDto), pero la repetimos aqui porque el dominio no puede
 * confiar en que siempre lo llamen desde HTTP.
 *
 * TODO: extiende RuntimeException con un mensaje claro.
 */
public class InvalidMovementValueException {

}
