package com.application.service.interfaces.rest.controller;

/**
 * PASO 7.1 - Controller de cuentas.
 *
 * Contract First: NO escribes @GetMapping, @PostMapping, rutas ni codigos de
 * estado. Todo eso vive en AccountsApi, generado del YAML. Tu solo implementas
 * la interfaz; si te falta un metodo, el compilador te avisa.
 *
 * Regenera las interfaces cuando cambies el contrato:  ./mvnw generate-sources
 *
 * TODO 0: @RestController @RequiredArgsConstructor
 *         public class AccountController implements AccountsApi
 *         inyecta AccountService, AccountMapper y el Scheduler jdbcScheduler.
 *
 * TODO 1: patron para CADA metodo (el controller debe quedar delgado):
 *         Mono.fromCallable(() -> service.loQueSea(...))   // trabajo bloqueante
 *             .subscribeOn(jdbcScheduler)                  // fuera del event loop
 *             .map(mapper::toDto)                          // dominio -> contrato
 *             .map(ResponseEntity::ok);
 *         Cuando el cuerpo llega como Mono<XxxDto>, primero .map(mapper::toDomain)
 *         o .flatMap(dto -> ...) para entrar al flujo.
 *
 * TODO 2: createAccount -> 201 con la cabecera Location:
 *         ResponseEntity.created(URI.create("/api/v1/accounts/" + dto.getAccountNumber())).body(dto)
 *
 * TODO 3: getAccount, listAccounts, updateAccount, patchAccount
 *
 * TODO 4: deleteAccount -> 204 sin cuerpo (ResponseEntity.noContent().build())
 *
 * TODO 5: NO pongas try/catch aqui. Los errores los traduce el
 *         GlobalExceptionHandler; el controller solo describe el camino feliz.
 */
public class AccountController {

}
