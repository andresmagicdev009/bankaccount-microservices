package com.application.service.domain.shared.constant;

/**
 * Codigo estable + plantilla de mensaje de cada error del dominio.
 *
 * El code() sale del nombre de la constante, asi que nunca se repite el literal.
 * El cliente puede comparar contra ese code sin parsear el texto del mensaje.
 *
 * REGLA: el comentario de cada constante dice cuantos argumentos espera.
 * String.format revienta con MissingFormatArgumentException si le faltan, y el
 * compilador no puede avisarte porque format(...) recibe Object...
 */
public enum ErrorCode {

    /** args: (nombreCampo, valor) */
    ACCOUNT_NOT_FOUND("Account not found with %s : '%s'"),
    /** args: (nombreCampo, valor) */
    MOVEMENT_NOT_FOUND("Movement not found with %s : '%s'"),
    /** args: (nombreCampo, valor) */
    CUSTOMER_NOT_FOUND("Customer not found with %s : '%s'"),

    /**
     * args: ninguno. Texto literal exigido por el enunciado (regla F3); no se
     * traduce ni se le agregan detalles. El detalle para depurar va al log.
     */
    INSUFFICIENT_BALANCE("Saldo no disponible"),

    /** args: (valorRecibido) */
    INVALID_MOVEMENT_VALUE("Movement value must be greater than zero, got: %s"),
    /** args: (accountNumber) */
    BALANCE_NOT_ZERO("Account %s cannot be deleted: its balance must be zero"),
    /** args: (startDate, endDate) */
    INVALID_DATE_RANGE("Start date %s must not be after end date %s"),
    /** args: (sizeRecibido) */
    INVALID_PAGE_SIZE("Page size must be between 1 and 100, got: %s"),
    /** args: (customerId) */
    CUSTOMER_SERVICE_UNAVAILABLE("Customer service unavailable for customer id: %s");

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
