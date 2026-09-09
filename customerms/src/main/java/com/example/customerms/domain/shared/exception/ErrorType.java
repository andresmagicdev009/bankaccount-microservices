package com.example.customerms.domain.shared.exception;

/**
 * Error category, expressed in domain language.
 *
 * The domain imports nothing from Spring or HTTP: it states WHAT kind of error
 * this is and the advice decides which status to answer with. That way the
 * mapping to 404/400/409 lives in a single place of the presentation layer, and
 * a new exception only has to pick its category.
 */
public enum ErrorType {

    /** The requested resource does not exist -> 404. */
    NOT_FOUND,

    /** The input data is not valid -> 400. */
    INVALID_INPUT,

    /** Clashes with the current state of the resource, e.g. a duplicate -> 409. */
    CONFLICT
}
