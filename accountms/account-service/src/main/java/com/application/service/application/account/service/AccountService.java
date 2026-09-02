package com.application.service.application.account.service;

/**
 * PASO 5.2 - Casos de uso de cuentas.
 *
 * Esta capa orquesta: no sabe de HTTP (no toca DTOs, ni ResponseEntity, ni
 * ServerWebExchange) y no sabe de SQL (habla con los puertos).
 *
 * Es bloqueante a proposito: el borde reactivo esta en el controller, y asi
 * @Transactional funciona sobre un solo hilo.
 *
 * TODO 0: anota @Service @RequiredArgsConstructor @Slf4j e inyecta por constructor
 *         private final AccountRepositoryPort repository;
 *         private final CustomerLookupPort customerLookup;
 *
 * TODO 1: create(Account account)  [@Transactional]
 *         a) valida que el cliente exista: customerLookup.findById(customerId)
 *            vacio -> lanza CustomerNotFoundException (404).
 *            Si el otro servicio esta caido, el adapter ya lanza la de 502: NO la captures.
 *         b) initialBalance null -> BigDecimal.ZERO
 *         c) availableBalance arranca igual al initialBalance
 *         d) status null -> true
 *         e) asigna el accountNumber (ver TODO 6)
 *         f) guarda y loguea
 *
 * TODO 2: findByAccountNumber(String accountNumber)
 *         repository...orElseThrow(() -> new AccountNotFoundException(accountNumber))
 *
 * TODO 3: findAll(Integer page, Integer size, String customerId)
 *         arma el Pageable con PageRequestFactory y delega en el puerto.
 *
 * TODO 4: update(...)  [@Transactional]  -> reemplazo total (PUT)
 *         Cuidado: el saldo NO se toca aqui. El saldo lo mandan los movimientos.
 *         Si cambia el customerId, valida que el nuevo cliente exista.
 *
 * TODO 5: patch(...)   [@Transactional]  -> solo aplica los campos NO nulos.
 *
 * TODO 6: delete(String accountNumber)  [@Transactional]
 *         si account.hasBalance() -> lanza AccountBalanceNotZeroException (409)
 *         si no, borra.
 *
 * TODO 7: generateAccountNumber() privado
 *         numero de 6 digitos (String.format("%06d", ...)) con SecureRandom,
 *         reintentando mientras repository.existsByAccountNumber(candidato) sea true.
 *         Limita los reintentos para no hacer un bucle infinito.
 */
public class AccountService {

}
