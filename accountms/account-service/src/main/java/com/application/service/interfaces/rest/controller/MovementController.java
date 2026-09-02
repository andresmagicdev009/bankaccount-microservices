package com.application.service.interfaces.rest.controller;

/**
 * PASO 7.2 - Controller de movimientos. implements MovementsApi
 *
 * TODO: mismo patron que AccountController, para
 *       createMovement (201 + Location), getMovement, listMovements,
 *       updateMovement, patchMovement, deleteMovement (204).
 *
 *       Detalle: el contrato define movementId como UUID en la ruta, pero el
 *       dominio lo maneja como String -> movementId.toString() al entrar.
 */
public class MovementController {

}
