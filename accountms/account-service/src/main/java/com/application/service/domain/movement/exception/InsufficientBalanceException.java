package com.application.service.domain.movement.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.BusinessRuleException;

/**
 * PASO 1.12 - Regla de negocio F3: saldo insuficiente -> 422.
 *
 * El mensaje que llega al cliente es EXACTAMENTE "Saldo no disponible": lo
 * define ErrorCode.INSUFFICIENT_BALANCE y no lleva argumentos. Si necesitas el
 * detalle (saldo disponible vs. solicitado) para depurar, va en el log del
 * MovementService, nunca en el cuerpo de la respuesta.
 */
public class InsufficientBalanceException extends BusinessRuleException {

    public InsufficientBalanceException() {
        super(ErrorCode.INSUFFICIENT_BALANCE);
    }
}
