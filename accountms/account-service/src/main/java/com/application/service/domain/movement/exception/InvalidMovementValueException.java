package com.application.service.domain.movement.exception;

/**
 * PASO 1.13 - Regla F2: el valor del movimiento debe ser estrictamente mayor a cero -> 400.
 *
 * Nota: la validacion tambien viaja en el contrato (@DecimalMin inclusive=false
 * en MovementCreateDto), pero la repetimos aqui porque el dominio no puede
 * confiar en que siempre lo llamen desde HTTP.
 *
 * Por que 400 y no 422: el YAML lo decide. POST /movements declara
 * '400': Invalid request (e.g. value is not greater than zero).
 * Suena a regla de negocio, pero se rechaza sin mirar el estado de la cuenta,
 * asi que es entrada invalida. El 422 queda solo para InsufficientBalanceException.
 *
 * TODO 1: extiende InvalidInputException, que vive en
 *         domain/shared/exception/ (esta clase se queda en movement/exception/:
 *         la categoria es compartida, la concreta pertenece al agregado).
 *
 * TODO 2: declara public static final String CODE = "INVALID_MOVEMENT_VALUE";
 *
 * TODO 3: constructor public que reciba el BigDecimal value y haga
 *         super(CODE, "Movement value must be greater than zero, got: " + value);
 */
public class InvalidMovementValueException {

}
