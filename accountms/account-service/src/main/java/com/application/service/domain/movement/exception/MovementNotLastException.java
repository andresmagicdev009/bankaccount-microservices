package com.application.service.domain.movement.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.StateConflictException;

/**
 * Only the last movement of an account can be edited or deleted -> 409.
 *
 * The balance field of each movement is the balance AFTER applying it: a frozen
 * historical value. Modifying a movement in the middle would leave every later
 * balance wrong and would force recomputing the whole chain. Blocking it is
 * cheaper and more honest than silently recomputing.
 *
 * Why 409 and not 422: the resource exists and the request is valid; what does
 * not allow the operation is the state -its position within the account-.
 */
public class MovementNotLastException extends StateConflictException {

    public MovementNotLastException(String movementId, String accountNumber) {
        super(ErrorCode.MOVEMENT_NOT_LAST, movementId, accountNumber);
    }
}
