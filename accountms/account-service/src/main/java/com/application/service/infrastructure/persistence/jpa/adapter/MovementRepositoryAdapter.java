package com.application.service.infrastructure.persistence.jpa.adapter;

/**
 * PASO 2.8 - Implementacion del puerto MovementRepositoryPort.
 *
 * TODO 0: @Component @RequiredArgsConstructor implements MovementRepositoryPort
 *
 * TODO 1: save / findById / deleteById -> mismo patron que el adaptador de cuentas.
 *
 * TODO 2: findAll(accountNumber, accountNumbers, from, to, pageable)
 *         arma la Specification con los filtros que NO sean null.
 *         Caso borde: si accountNumbers viene vacia (cliente sin cuentas), el
 *         resultado debe ser vacio, no "todos". Con criteria: builder.disjunction().
 *
 * TODO 3: findByAccountAndRange -> el derived query ordenado por fecha ascendente.
 */
public class MovementRepositoryAdapter {

}
