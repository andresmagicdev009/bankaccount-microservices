package com.application.service.domain.movement.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.ResourceNotFoundException;

/** PASO 1.11 - Movimiento inexistente -> 404. */
public class MovementNotFoundException extends ResourceNotFoundException {

    public MovementNotFoundException(String movementId) {
        super(ErrorCode.MOVEMENT_NOT_FOUND, "movementId", movementId);
    }
}
