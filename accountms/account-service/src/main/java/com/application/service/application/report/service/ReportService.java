package com.application.service.application.report.service;

/**
 * PASO 5.5 - Caso de uso del estado de cuenta.
 *
 * Es el unico que combina dos fuentes: lo nuestro (cuentas + movimientos) y lo
 * del microservicio de clientes (nombre e identificacion).
 *
 * TODO 0: @Service @RequiredArgsConstructor @Slf4j
 *         AccountRepositoryPort, MovementRepositoryPort, CustomerLookupPort
 *
 * TODO 1: generate(String customerId, LocalDate startDate, LocalDate endDate)
 *         [@Transactional(readOnly = true)]
 *         a) startDate > endDate -> InvalidDateRangeException (400)
 *         b) customerLookup.findById(...) vacio -> CustomerNotFoundException (404)
 *         c) accountRepository.findByCustomerId(...)
 *            si la lista viene vacia -> tambien 404 (asi lo pide el contrato:
 *            "el cliente no existe o no tiene cuentas asociadas")
 *         d) por cada cuenta, movimientos del rango con findByAccountAndRange
 *            (usa un LinkedHashMap para conservar el orden de las cuentas)
 *         e) arma y devuelve el AccountStatement
 */
public class ReportService {

}
