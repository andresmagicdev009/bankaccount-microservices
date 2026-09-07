package com.application.service.interfaces.rest.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.application.service.application.account.service.AccountService;
import com.application.service.interfaces.rest.helpers.BlockingBridge;
import com.application.service.interfaces.rest.helpers.ResourceLocation;
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

/**
 * PASO 7.1 - Controller de cuentas.
 *
 * Las rutas y los codigos de estado vienen de AccountsApi, generada del
 * contrato: aqui no hay ni un @GetMapping ni un @RequestMapping.
 *
 * Este es el borde reactivo. El trabajo bloqueante (JPA) no se ejecuta en el
 * event loop: pasa por BlockingBridge, que lo empuja al jdbcScheduler.
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
    private final BlockingBridge blocking;

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
                .flatMap(account -> blocking.call(() -> accountService.create(account)))
                .map(accountMapper::toDto)
                .map(dto -> ResponseEntity
                        .created(ResourceLocation.of(exchange, dto.getAccountNumber()))
                        .body(dto));
    }

    @Override
    public Mono<ResponseEntity<AccountDto>> getAccount(String accountNumber, ServerWebExchange exchange) {
        return blocking.call(() -> accountService.get(accountNumber))
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

        return blocking.call(() -> accountService.list(customer, page, size))
                .map(accountMapper::toPageDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountDto>> updateAccount(String accountNumber,
            Mono<AccountUpdateDto> accountUpdateDto, ServerWebExchange exchange) {
        return accountUpdateDto
                .map(accountMapper::toDomain)
                .flatMap(changes -> blocking.call(() -> accountService.update(accountNumber, changes)))
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
                .flatMap(dto -> blocking.call(() -> accountService.patch(accountNumber,
                        accountMapper.toDomainType(dto.getAccountType()),
                        dto.getStatus())))
                .map(accountMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /** delete no devuelve nada: el Mono vacio dispara el 204. */
    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(String accountNumber, ServerWebExchange exchange) {
        return blocking.run(() -> accountService.delete(accountNumber))
                .thenReturn(ResponseEntity.noContent().build());
    }
}
