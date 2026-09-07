package com.example.customerms.domain.shared.exception;

/**
 * Categoria del error, en lenguaje de dominio.
 *
 * El dominio no importa nada de Spring ni de HTTP: dice QUE clase de error es y
 * el advice decide con que status responderlo. Asi el mapeo a 404/400/409 vive
 * en un solo sitio de la capa de presentacion, y una excepcion nueva solo tiene
 * que elegir su categoria.
 */
public enum ErrorType {

    /** El recurso pedido no existe -> 404. */
    NOT_FOUND,

    /** Los datos de entrada no son validos -> 400. */
    INVALID_INPUT,

    /** Choca con el estado actual del recurso, p.ej. un duplicado -> 409. */
    CONFLICT
}
