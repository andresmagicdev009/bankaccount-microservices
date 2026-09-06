package com.application.service.domain.account.exception;

import com.application.service.domain.shared.constant.ErrorCode;
import com.application.service.domain.shared.exception.DomainException;

/**
 * Se acabaron los numeros de cuenta del dominio.
 *
 * Cuelga de DomainException y no de ninguna de las cuatro categorias a
 * proposito: no es culpa del cliente -no hay nada que pueda corregir en su
 * peticion-, es capacidad agotada del sistema. Al no encajar en 400/404/409/422
 * la recoge el @ExceptionHandler(Exception.class) de GlobalExceptionHandler y
 * sale como 500 con el texto generico, que es justo lo que se quiere: el
 * detalle queda en el log, no en la respuesta.
 */
public class AccountNumberExhaustedException extends DomainException {

    public AccountNumberExhaustedException() {
        super(ErrorCode.ACCOUNT_NUMBER_EXHAUSTED.code(),
                ErrorCode.ACCOUNT_NUMBER_EXHAUSTED.format());
    }
}
