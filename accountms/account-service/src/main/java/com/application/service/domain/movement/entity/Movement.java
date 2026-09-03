package com.application.service.domain.movement.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PASO 1.4 - Modelo de dominio del movimiento.
 *
 * balance guarda el saldo de la cuenta DESPUES de aplicar este movimiento: es
 * un dato historico congelado que pide el contrato y usa el reporte.
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

    /** Convierte tipo + monto en un numero con signo, listo para sumar al saldo. */
    public BigDecimal signedValue() {
        return movementType.signed(value);
    }
}
