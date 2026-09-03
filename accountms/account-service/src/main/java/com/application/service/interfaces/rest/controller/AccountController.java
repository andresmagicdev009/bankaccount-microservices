package com.application.service.interfaces.rest.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.application.service.application.account.service.AccountService;
import com.application.service.interfaces.rest.api.AccountsApi;
import com.application.service.interfaces.rest.dto.AccountCreateDto;
import com.application.service.interfaces.rest.dto.AccountDto;
import com.application.service.interfaces.rest.dto.AccountPageDto;
import com.application.service.interfaces.rest.dto.AccountPatchDto;
import com.application.service.interfaces.rest.dto.AccountUpdateDto;
import com.application.service.interfaces.rest.mapper.AccountMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * PASO 7.1 - Controller de cuentas.
 *
 * Las rutas y los codigos de estado vienen de AccountsApi, generada del contrato:
 * aqui no hay ni un @GetMapping ni un @RequestMapping.
 *
 * Este es el borde reactivo. El proyecto es WebFlux (event loop de pocos hilos)
 * pero la persistencia es JPA, que bloquea. Cada llamada al servicio se envuelve
 * en Mono.fromCallable(...).subscribeOn(jdbcScheduler) para que el trabajo
 * bloqueante salga del event loop; si se ejecutara ahi, un par de consultas
 * lentas dejarian sin atender al servidor entero.
 *
 * No hay try/catch: las excepciones de dominio suben hasta GlobalExceptionHandler.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AccountController implements AccountsApi {

    private final AccountService service;
    private final AccountMapper mapper;
    private final Scheduler jdbcScheduler;

    @Override
    public Mono<ResponseEntity<AccountDto>> createAccount(Mono<AccountCreateDto> accountCreateDto,
            ServerWebExchange exchange) {
        return accountCreateDto
                .map(mapper::toDomain)
                .flatMap(account -> Mono.fromCallable(() -> service.create(account))
                        .subscribeOn(jdbcScheduler))
                .map(mapper::toDto)
                .map(dto -> ResponseEntity
                        .created(URI.create("/api/v1/accounts/" + dto.getAccountNumber()))
                        .body(dto));
    }

    @Override
    public Mono<ResponseEntity<AccountDto>> getAccount(String accountNumber, ServerWebExchange exchange) {
        return Mono.fromCallable(() -> service.findByAccountNumber(accountNumber))
                .subscribeOn(jdbcScheduler)
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountPageDto>> listAccounts(Integer page, Integer size, UUID customerId,
            ServerWebExchange exchange) {
        String customer = (customerId == null) ? null : customerId.toString();

        return Mono.fromCallable(() -> service.findAll(page, size, customer))
                .subscribeOn(jdbcScheduler)
                .map(mapper::toPageDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountDto>> updateAccount(String accountNumber,
            Mono<AccountUpdateDto> accountUpdateDto, ServerWebExchange exchange) {
        return accountUpdateDto
                .map(mapper::toDomain)
                .flatMap(changes -> Mono.fromCallable(() -> service.update(accountNumber, changes))
                        .subscribeOn(jdbcScheduler))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * El patch pasa los campos sueltos al servicio, no el DTO: la capa de
     * aplicacion no puede importar tipos del contrato.
     */
    @Override
    public Mono<ResponseEntity<AccountDto>> patchAccount(String accountNumber,
            Mono<AccountPatchDto> accountPatchDto, ServerWebExchange exchange) {
        return accountPatchDto
                .flatMap(dto -> Mono
                        .fromCallable(() -> service.patch(accountNumber,
                                mapper.toDomainType(dto), dto.getStatus()))
                        .subscribeOn(jdbcScheduler))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * fromRunnable no emite ningun elemento, asi que el .map(...) de los demas
     * metodos nunca correria. thenReturn produce el 204 cuando el borrado termina.
     */
    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(String accountNumber, ServerWebExchange exchange) {
        return Mono.<Void>fromRunnable(() -> service.delete(accountNumber))
                .subscribeOn(jdbcScheduler)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
