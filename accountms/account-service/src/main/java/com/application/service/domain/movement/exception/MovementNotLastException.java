package com.application.service.domain.movement.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.StateConflictException;

/**
 * PASO 1.14 - Solo el ultimo movimiento de una cuenta se puede editar o borrar
 * -> 409.
 *
 * El campo balance de cada movimiento es el saldo DESPUES de aplicarlo: un dato
 * historico congelado. Si se modificara un movimiento intermedio, todos los
 * balance posteriores quedarian mal y habria que recalcular la cadena entera.
 * Bloquearlo es mas barato y mas honesto que recalcular en silencio.
 *
 * Por que 409 y no 422: el recurso existe y la peticion es valida; lo que no
 * permite la operacion es el estado -su posicion en la cuenta-.
 */
public class MovementNotLastException extends StateConflictException {

    public MovementNotLastException(String movementId, String accountNumber) {
        super(ErrorCode.MOVEMENT_NOT_LAST, movementId, accountNumber);
    }
}
