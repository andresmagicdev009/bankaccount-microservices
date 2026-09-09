package com.application.service.application.account.model;

import java.math.BigDecimal;

import com.application.service.domain.account.entity.Account;

/**
 * An account plus its available balance, already resolved.
 *
 * It exists because the available balance is not a field of Account: the
 * specification models it as the "saldo" field of the movement. The contract,
 * however, DOES expose it in the response (availableBalance is readOnly), so
 * somebody has to put the two together without polluting the domain. That
 * somebody is this record.
 *
 * It lives in application and not in domain on purpose: it is a use-case
 * result, not a business rule.
 *
 * @param account          the account exactly as persisted
 * @param availableBalance balance of the last movement; the initial one when
 *                         there is none
 */
public record AccountView(Account account, BigDecimal availableBalance) {
}
