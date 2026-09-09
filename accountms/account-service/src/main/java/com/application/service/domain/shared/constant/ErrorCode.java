package com.application.service.domain.shared.constant;

/**
 * Stable code + message template of every domain error.
 *
 * code() comes from the name of the constant, so the literal is never repeated.
 * A client can compare against that code without parsing the message text.
 *
 * RULE: the comment on each constant states how many arguments it expects.
 * String.format blows up with MissingFormatArgumentException when they are
 * missing, and the compiler cannot warn about it because format(...) takes an
 * Object...
 */
public enum ErrorCode {

    /** args: (fieldName, value) */
    ACCOUNT_NOT_FOUND("Account not found with %s : '%s'"),
    /** args: (fieldName, value) */
    MOVEMENT_NOT_FOUND("Movement not found with %s : '%s'"),
    /** args: (fieldName, value) */
    CUSTOMER_NOT_FOUND("Customer not found with %s : '%s'"),

    /**
     * args: none. Literal text required by the specification (rule F3); it is
     * neither translated nor extended with details. The debugging detail goes
     * to the log.
     */
    INSUFFICIENT_BALANCE("Saldo no disponible"),

    /** args: (receivedValue) */
    INVALID_MOVEMENT_VALUE("Movement value must be greater than zero, got: %s"),
    /**
     * args: (movementId, accountNumber). Only the last movement of an account
     * can be edited or deleted: the balance of each row is historical and
     * touching one in the middle would unbalance every later row.
     */
    MOVEMENT_NOT_LAST("Movement %s is not the last movement of account %s; only the last one can be modified or deleted"),

    /** args: (accountNumber) */
    BALANCE_NOT_ZERO("Account %s cannot be deleted: its balance must be zero"),
    /**
     * args: (accountNumber). An inactive account still exists -that is why this
     * is not a 404-: it is its state that does not accept movements.
     */
    ACCOUNT_INACTIVE("Account %s does not accept movements: it is inactive"),
    /** args: (startDate, endDate) */
    INVALID_DATE_RANGE("Start date %s must not be after end date %s"),
    /** args: (receivedSize) */
    INVALID_PAGE_SIZE("Page size must be between 1 and 100, got: %s"),
    /**
     * args: none. The sequence went past the size of the domain: no free
     * account numbers are left. It is not a client error, it goes out as a 500.
     */
    ACCOUNT_NUMBER_EXHAUSTED("Account number space is exhausted"),

    /** args: (customerId) */
    CUSTOMER_SERVICE_UNAVAILABLE("Customer service is down 😶‍🌫️.");

    private final String template;

    ErrorCode(String template) {
        this.template = template;
    }

    public String format(Object... args) {
        return String.format(template, args);
    }

    public String code() {
        return name();
    }
}
