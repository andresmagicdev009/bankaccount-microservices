package com.application.service.domain.movement.entity;

/**
 * PASO 1.4 - Modelo de dominio del movimiento.
 *
 * TODO 1: declara los campos
 *         movementId (String)     -> UUID en texto, lo genera MovementService
 *         date (LocalDateTime)
 *         movementType (MovementType)
 *         value (BigDecimal)      -> monto, siempre > 0
 *         balance (BigDecimal)    -> saldo de la cuenta DESPUES de aplicar este movimiento
 *         accountNumber (String)
 *
 * TODO 2: anota con Lombok igual que Account.
 *
 * TODO 3: metodo signedValue() que devuelva movementType.signed(value).
 *         Lo usaran el create, el update y el delete (para revertir).
 */
public class Movement {

}
