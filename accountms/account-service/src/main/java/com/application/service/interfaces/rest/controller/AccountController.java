package com.application.service.interfaces.rest.controller;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Callable;

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
 * Las rutas y los codigos de estado vienen de AccountsApi, generada del
 * contrato: aqui no hay ni un @GetMapping ni un @RequestMapping.
 *
 * Este es el borde reactivo. El proyecto es WebFlux (event loop de pocos hilos)
 * pero la persistencia es JPA, que bloquea. Cada llamada al servicio se envuelve
 * en Mono.fromCallable(...).subscribeOn(jdbcScheduler) para que el trabajo
 * bloqueante salga del event loop; si se ejecutara ahi, un par de consultas
 * lentas dejarian sin atender al servidor entero.
 *
 * No hay try/catch: las excepciones de dominio suben hasta
 * GlobalExceptionHandler.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AccountController implements AccountsApi {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final Scheduler jdbcScheduler;

    /**
     * POST /accounts -> 201 con cabecera Location.
     *
     * El body llega como Mono porque el contrato es reactivo: hasta que no se
     * suscribe no hay DTO. De ahi el map para traducirlo y el flatMap para
     * encadenar el trabajo bloqueante, que ya devuelve otro Mono.
     */
    @Override
    public Mono<ResponseEntity<AccountDto>> createAccount(Mono<AccountCreateDto> accountCreateDto,
            ServerWebExchange exchange) {
        return accountCreateDto
                .map(accountMapper::toDomain)
                .flatMap(account -> blocking(() -> accountService.create(account)))
                .map(accountMapper::toDto)
                .map(dto -> ResponseEntity
                        .created(location(exchange, dto.getAccountNumber()))
                        .body(dto));
    }

    @Override
    public Mono<ResponseEntity<AccountDto>> getAccount(String accountNumber, ServerWebExchange exchange) {
        return blocking(() -> accountService.get(accountNumber))
                .map(accountMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * El contrato declara customerId como UUID y el dominio lo guarda como
     * String: se convierte aqui, no en el servicio. Null = sin filtro.
     */
    @Override
    public Mono<ResponseEntity<AccountPageDto>> listAccounts(Integer page, Integer size, UUID customerId,
            ServerWebExchange exchange) {
        String customer = (customerId == null) ? null : customerId.toString();

        return blocking(() -> accountService.list(customer, page, size))
                .map(accountMapper::toPageDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountDto>> updateAccount(String accountNumber,
            Mono<AccountUpdateDto> accountUpdateDto, ServerWebExchange exchange) {
        return accountUpdateDto
                .map(accountMapper::toDomain)
                .flatMap(changes -> blocking(() -> accountService.update(accountNumber, changes)))
                .map(accountMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * El PATCH no pasa por un Account: sus campos nulos significan "no cambies
     * esto", y un Account a medio llenar no sabria distinguir eso de un "ponlo
     * a null". Por eso el servicio recibe los dos campos sueltos.
     */
    @Override
    public Mono<ResponseEntity<AccountDto>> patchAccount(String accountNumber,
            Mono<AccountPatchDto> accountPatchDto, ServerWebExchange exchange) {
        return accountPatchDto
                .flatMap(dto -> blocking(() -> accountService.patch(accountNumber,
                        accountMapper.toDomainType(dto.getAccountType()),
                        dto.getStatus())))
                .map(accountMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * delete no devuelve nada, pero Mono.fromCallable no admite null: se
     * devuelve un booleano de relleno que solo sirve para disparar el 204.
     */
    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(String accountNumber, ServerWebExchange exchange) {
        return blocking(() -> {
            accountService.delete(accountNumber);
            return true;
        }).map(deleted -> ResponseEntity.noContent().build());
    }

    /**
     * Unico sitio que empuja el trabajo bloqueante fuera del event loop.
     *
     * fromCallable y no just(...): con just, la llamada se ejecutaria al armar
     * el pipeline -en el event loop- y subscribeOn no serviria de nada.
     */
    private <T> Mono<T> blocking(Callable<T> work) {
        return Mono.fromCallable(work).subscribeOn(jdbcScheduler);
    }

    /**
     * Location del 201, construida sobre la ruta de la peticion para que
     * incluya el base-path (/api/v1) sin repetirlo aqui.
     */
    private URI location(ServerWebExchange exchange, String accountNumber) {
        return URI.create(exchange.getRequest().getPath().value() + "/" + accountNumber);
    }
}
