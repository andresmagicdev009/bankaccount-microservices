package com.application.service.application.account.service;

import java.math.BigDecimal;
import java.security.SecureRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.service.application.account.model.AccountView;
import com.application.service.application.shared.PageRequestFactory;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.entity.AccountType;
import com.application.service.domain.account.exception.AccountBalanceNotZeroException;
import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.customer.entity.CustomerSnapshot;
import com.application.service.domain.customer.exception.CustomerNotFoundException;
import com.application.service.domain.customer.port.CustomerLookupPort;
import com.application.service.domain.movement.repository.MovementRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PASO 5.2 - Casos de uso de cuentas.
 *
 * Esta capa orquesta: no sabe de HTTP (no toca DTOs, ni ResponseEntity, ni
 * ServerWebExchange) y no sabe de SQL (habla con los puertos).
 *
 * Es bloqueante a proposito: el borde reactivo esta en el controller, y asi
 * @Transactional funciona sobre un solo hilo.
 *
 * Todo metodo de lectura devuelve AccountView y no Account: el contrato expone
 * availableBalance, que no es columna de account sino el balance del ultimo
 * movimiento. Resolverlo aqui evita que el mapper tenga que tocar repositorios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    /** 6 digitos como los ejemplos del enunciado (225487, 478758). */
    private static final int ACCOUNT_NUMBER_DIGITS = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Orden por defecto del listado: la mas reciente primero. */
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final AccountRepositoryPort accountRepository;
    private final MovementRepositoryPort movementRepository;
    private final CustomerLookupPort customerLookup;


    private static final int ACCOUNT_NUMBER_ORIGIN = 100_000;
    private static final int ACCOUNT_NUMBER_BOUND = 1_000_000;
    private static final int MAX_GENERATION_ATTEMPTS = 10;
    

    // ------------------------------------------------------------------ CREATE

    /**
     * POST /accounts.
     *
     * TODO:
     * 1. validar que el cliente existe: customerLookup.findById(customerId)
     * vacio -> CustomerNotFoundException (404). Si el otro microservicio no
     * responde, el puerto ya lanza CustomerServiceUnavailableException (502),
     * no lo captures aqui.
     * 2. asignar accountNumber con generateAccountNumber() (el contrato lo marca
     * readOnly: si viene en el body se ignora).
     * 3. status null -> true por defecto.
     * 4. guardar y devolver toView(guardada). Recien creada no tiene
     * movimientos, asi que availableBalance == initialBalance.
     */
    @Transactional
    public AccountView create(Account account) {


        CustomerSnapshot customer = customerLookup.findById(account.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(account.getCustomerId()));
        
        Account newAccount = Account.builder()
                .accountNumber(generateAccountNumber())
                .accountType(account.getAccountType())
                .initialBalance(account.getInitialBalance())
                .status(account.getStatus() == null ? true : account.getStatus())
                .customerId(customer.getCustomerId())
                .build();
        return toView(accountRepository.save(newAccount));
        

    }

    // -------------------------------------------------------------------- READ

    /**
     * GET /accounts/{accountNumber}.
     *
     * TODO: findByAccountNumber, orElseThrow AccountNotFoundException, toView.
     */
    @Transactional(readOnly = true)
    public AccountView get(String accountNumber) {
        return toView(accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber)));
    }

    /**
     * GET /accounts?page&size&customerId.
     *
     * TODO: PageRequestFactory.of(page, size, DEFAULT_SORT) ->
     * accountRepository.findAll(customerId, pageable) -> page.map(this::toView).
     *
     * Ojo: un toView por fila son N consultas de saldo. Aceptable para este
     * proyecto; si molestara, se resuelve con un findLatestBalances(List) en el
     * puerto de movimientos.
     */
    @Transactional(readOnly = true)
    public Page<AccountView> list(String customerId, Integer page, Integer size) {
        Pageable pageable = PageRequestFactory.of(page, size, DEFAULT_SORT);
        return accountRepository.findAll(customerId, pageable)
                .map(this::toView);
    }

    // ------------------------------------------------------------------ UPDATE

    /**
     * PUT /accounts/{accountNumber} - reemplazo total.
     *
     * TODO:
     * 1. cargar la existente o AccountNotFoundException.
     * 2. si cambia el customerId, revalidarlo contra customerLookup.
     * 3. pisar accountType, status y customerId. initialBalance NO se toca:
     * es readOnly en AccountUpdate y mutarlo descuadraria los movimientos.
     * 4. save + toView.
     */
    @Transactional
    public AccountView update(String accountNumber, Account changes) {
        Account existingAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (changes.getCustomerId() != null && !changes.getCustomerId().equals(existingAccount.getCustomerId())) {
            customerLookup.findById(changes.getCustomerId())
                    .orElseThrow(() -> new CustomerNotFoundException(changes.getCustomerId()));
            existingAccount.setCustomerId(changes.getCustomerId());
        }

        if (changes.getAccountType() != null) {
            existingAccount.setAccountType(changes.getAccountType());
        }

        if (changes.getStatus() != null) {
            existingAccount.setStatus(changes.getStatus());
        }

        return toView(accountRepository.save(existingAccount));
    }

    /**
     * PATCH /accounts/{accountNumber} - parcial.
     *
     * TODO: cargar o 404; aplicar solo los parametros no nulos (null = conserva
     * el valor actual); save + toView.
     */
    @Transactional
    public AccountView patch(String accountNumber, AccountType accountType, Boolean status) {
        
        Account existingAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (accountType != null) {
            existingAccount.setAccountType(accountType);
        }

        if (status != null) {
            existingAccount.setStatus(status);
        }

        return toView(accountRepository.save(existingAccount));
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * DELETE /accounts/{accountNumber}.
     *
     * TODO:
     * 1. cargar o AccountNotFoundException (404).
     * 2. regla de negocio: si el saldo disponible != 0 ->
     * AccountBalanceNotZeroException (409). Compara con
     * BigDecimal.compareTo, nunca con equals (2.0 no es equals a 2.00).
     * 3. accountRepository.deleteByAccountNumber(...) -que ya borra antes los
     * movimientos-.
     */
    @Transactional
    public void delete(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        BigDecimal latestBalance = movementRepository.findLatestBalance(accountNumber)
                .orElse(account.getInitialBalance());

        if (latestBalance.compareTo(BigDecimal.ZERO) != 0) {
            throw new AccountBalanceNotZeroException(accountNumber);
        }

        accountRepository.deleteByAccountNumber(accountNumber);
    }

    // ----------------------------------------------------------------- HELPERS

    /**
     * Une la cuenta con su saldo disponible.
     *
     * TODO: movementRepository.findLatestBalance(numero)
     * .orElse(account.getInitialBalance()) y envolver en AccountView.
     */
    private AccountView toView(Account account) {
        BigDecimal latestBalance = movementRepository.findLatestBalance(account.getAccountNumber())
                .orElse(account.getInitialBalance());
        return new AccountView(account, latestBalance);
    }

    /**
     * Numero de cuenta aleatorio de 6 digitos, reintentando hasta que no exista.
     *
     * TODO: bucle con existsByAccountNumber; pon un tope de intentos para no
     * dejar un while(true) si el espacio se llenara.
     */
    private String generateAccountNumber() {


        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = String.valueOf(
                    RANDOM.nextInt(ACCOUNT_NUMBER_ORIGIN, ACCOUNT_NUMBER_BOUND));

            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
            log.debug("Account number {} already taken, retrying {}/{}",
                    candidate, attempt, MAX_GENERATION_ATTEMPTS);
        }

        throw new IllegalStateException(
                "Could not generate a free account number after "
                        + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}
