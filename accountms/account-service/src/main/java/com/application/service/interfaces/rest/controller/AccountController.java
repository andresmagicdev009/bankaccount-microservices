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
 * Accounts controller.
 *
 * The routes and the status codes come from AccountsApi, generated from the
 * contract: there is not a single @GetMapping or @RequestMapping here.
 *
 * This is the reactive edge. The blocking work (JPA) does not run on the event
 * loop: it goes through BlockingBridge, which pushes it to the jdbcScheduler.
 *
 * There is no try/catch: domain exceptions travel up to
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
     * POST /accounts -> 201 with a Location header.
     *
     * The body arrives as a Mono because the contract is reactive: there is no
     * DTO until it is subscribed to. Hence the map to translate it and the
     * flatMap to chain the blocking work, which already returns another Mono.
     */
    @Override
    public Mono<ResponseEntity<AccountDto>> postAccount(Mono<AccountCreateDto> accountCreateDto,
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
     * The contract declares customerId as a UUID and the domain stores it as a
     * String: the conversion happens here, not in the service. Null means no
     * filter.
     */
    @Override
    public Mono<ResponseEntity<AccountPageDto>> getAccounts(Integer page, Integer size, UUID customerId,
            ServerWebExchange exchange) {
        String customer = (customerId == null) ? null : customerId.toString();

        return blocking.call(() -> accountService.list(customer, page, size))
                .map(accountMapper::toPageDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<AccountDto>> putAccount(String accountNumber,
            Mono<AccountUpdateDto> accountUpdateDto, ServerWebExchange exchange) {
        return accountUpdateDto
                .map(accountMapper::toDomain)
                .flatMap(changes -> blocking.call(() -> accountService.update(accountNumber, changes)))
                .map(accountMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * The PATCH does not go through a Account: its null fields mean "do not change
     * this", and a half-filled Account could not tell that apart from "set it to
     * null". That is why the service receives the two fields loose.
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

    /** delete returns nothing: the empty Mono triggers the 204. */
    @Override
    public Mono<ResponseEntity<Void>> deleteAccount(String accountNumber, ServerWebExchange exchange) {
        return blocking.run(() -> accountService.delete(accountNumber))
                .thenReturn(ResponseEntity.noContent().build());
    }
}
