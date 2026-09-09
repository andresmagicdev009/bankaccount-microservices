package com.application.service.domain.account.entity;

/**
 * Account types.
 *
 * The constants are named exactly like the enum of the contract
 * (components/schemas/Account.accountType), so the REST mapper can translate
 * them with valueOf(...) and no conversion tables.
 */
public enum AccountType {
    SAVINGS, CHECKING
}
