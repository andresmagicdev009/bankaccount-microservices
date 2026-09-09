package com.application.service.domain.movement.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain model of the movement.
 *
 * balance holds the balance of the account AFTER applying this movement: a
 * frozen historical value required by the contract and used by the report.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movement {

    private String movementId;
    private LocalDateTime date;
    private MovementType movementType;
    private BigDecimal value;
    private BigDecimal balance;
    private String accountNumber;

    /** Turns type + amount into a signed number, ready to add to the balance. */
    public BigDecimal signedValue() {
        return movementType.signed(value);
    }
}
