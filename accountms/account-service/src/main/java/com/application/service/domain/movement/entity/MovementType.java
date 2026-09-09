package com.application.service.domain.movement.entity;

import java.math.BigDecimal;

/**
 * Movement types.
 *
 * signed(BigDecimal) is rule F2 of the specification turned into code:
 *   DEBIT  -> returns value.negate()  (subtracts from the balance)
 *   CREDIT -> returns value           (adds to the balance)
 * Keeping it here avoids repeating an if/else in the service.
 */
public enum MovementType {
    DEBIT, CREDIT;

    public BigDecimal signed(BigDecimal value){
        return this == DEBIT ? value.negate() : value;
    }
}
