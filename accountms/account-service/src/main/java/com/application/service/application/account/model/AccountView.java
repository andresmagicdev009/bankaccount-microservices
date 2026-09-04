package com.application.service.application.account.model;

import java.math.BigDecimal;

import com.application.service.domain.account.entity.Account;

/**
 * PASO 5.1 - Cuenta mas su saldo disponible, ya resuelto.
 *
 * Existe porque el saldo disponible no es un campo de Account: el enunciado lo
 * modela como el campo "saldo" del movimiento. Pero el contrato SI lo expone en
 * la respuesta (availableBalance es readOnly), asi que alguien tiene que juntar
 * las dos cosas sin ensuciar el dominio. Ese alguien es este record.
 *
 * Vive en application y no en domain a proposito: es un resultado de caso de
 * uso, no una regla de negocio.
 *
 * @param account          la cuenta tal como esta persistida
 * @param availableBalance saldo del ultimo movimiento; el inicial si no tiene
 */
public record AccountView(Account account, BigDecimal availableBalance) {
}
