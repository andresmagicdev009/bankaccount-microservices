package com.application.service.application.movement.service;

/**
 * PASO 5.3 - Casos de uso de movimientos. Aqui viven las reglas F2 y F3.
 *
 * TODO 0: @Service @RequiredArgsConstructor @Slf4j
 *         private final MovementRepositoryPort repository;
 *         private final AccountRepositoryPort accountRepository;
 *
 * TODO 1: metodo privado applyToBalance(Account account, BigDecimal signedAmount)
 *         >>> Este es el corazon del servicio. Escribelo PRIMERO. <<<
 *         a) nuevoSaldo = account.getAvailableBalance().add(signedAmount)
 *         b) si nuevoSaldo < 0  -> lanza InsufficientBalanceException  (422 "Saldo no disponible")
 *         c) setea el saldo en la cuenta, la guarda con accountRepository.save(...)
 *         d) devuelve el nuevoSaldo (se guarda en el campo balance del movimiento)
 *         Todo lo demas (create/update/delete) pasa por aqui: una sola puerta,
 *         una sola validacion de saldo.
 *
 * TODO 2: metodo privado requirePositiveValue(BigDecimal value)
 *         null o <= 0 -> InvalidMovementValueException  (regla F2)
 *
 * TODO 3: create(Movement movement)  [@Transactional]
 *         valida el valor -> busca la cuenta (404 si no existe) ->
 *         applyToBalance(cuenta, movement.signedValue()) ->
 *         genera movementId (UUID.randomUUID().toString()), fecha now(), balance ->
 *         guarda y loguea.
 *
 * TODO 4: findById(String movementId) -> orElseThrow(MovementNotFoundException)
 *
 * TODO 5: findAll(page, size, accountNumber, customerId, startDate, endDate)
 *         a) si startDate > endDate -> InvalidDateRangeException
 *         b) si viene customerId: NO llames al otro microservicio. Este servicio
 *            ya sabe que cuentas son de ese cliente:
 *            accountRepository.findByCustomerId(customerId) -> lista de numeros.
 *         c) convierte LocalDate a LocalDateTime: inicio -> atStartOfDay(),
 *            fin -> atTime(LocalTime.MAX), si no pierdes los movimientos del ultimo dia.
 *         d) ordena por fecha descendente.
 *
 * TODO 6: update(movementId, movementType, value)  [@Transactional]
 *         Es un reemplazo, asi que hay que revertir el efecto viejo antes de
 *         aplicar el nuevo: delta = nuevoSigned - viejoSigned, y pasas el delta
 *         a applyToBalance. Asi la validacion de saldo sigue siendo una sola.
 *
 * TODO 7: patch(movementId, movementType, value)  [@Transactional]
 *         los null se quedan con el valor actual, y luego reusa update(...).
 *
 * TODO 8: delete(String movementId)  [@Transactional]
 *         borrar un movimiento revierte su efecto:
 *         applyToBalance(cuenta, movimiento.signedValue().negate()) y luego borra.
 */
public class MovementService {

}
