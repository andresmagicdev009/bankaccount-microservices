package com.application.service.application.shared;

/**
 * PASO 5.1 - Utilidad compartida de paginacion.
 *
 * Existe para que los tres servicios validen page/size igual y no repitas la
 * misma logica tres veces.
 *
 * TODO 1: clase final con constructor privado (es una utilidad, no un bean).
 * TODO 2: metodo estatico
 *         public static Pageable of(Integer page, Integer size, Sort sort)
 *         - page null o negativo -> 0
 *         - size fuera de 1..100 -> lanza InvalidPageSizeException
 *         - size null            -> 20 (el default del contrato)
 *         - devuelve PageRequest.of(pageNumber, pageSize, sort)
 */
public final class PageRequestFactory {

}
