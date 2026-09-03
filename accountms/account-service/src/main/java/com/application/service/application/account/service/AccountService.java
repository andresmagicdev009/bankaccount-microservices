package com.application.service.application.account.service;

import java.math.BigDecimal;
import java.security.SecureRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.application.service.application.shared.PageRequestFactory;
import com.application.service.domain.account.entity.Account;
import com.application.service.domain.account.entity.AccountType;
import com.application.service.domain.account.exception.AccountBalanceNotZeroException;
import com.application.service.domain.account.exception.AccountNotFoundException;
import com.application.service.domain.account.repository.AccountRepositoryPort;
import com.application.service.domain.customer.exception.CustomerNotFoundException;
import com.application.service.domain.customer.port.CustomerLookupPort;

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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ACCOUNT_NUMBER_BOUND = 1_000_000;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private final AccountRepositoryPort repository;
    private final CustomerLookupPort customerLookup;

    /**
     * El cliente se valida contra el otro microservicio ANTES de insertar: si no
     * existe es un 404. Si ese servicio esta caido, CustomerLookupAdapter lanza
     * CustomerServiceUnavailableException (502) y aqui NO se captura a proposito.
     */
    @Transactional
    public Account create(Account account) {
        requireExistingCustomer(account.getCustomerId());

        BigDecimal initialBalance = (account.getInitialBalance() == null)
                ? BigDecimal.ZERO
                : account.getInitialBalance();

        account.setInitialBalance(initialBalance);
        // Cuenta recien creada: sin movimientos todavia, el saldo disponible es el inicial.
        account.setAvailableBalance(initialBalance);
        if (account.getStatus() == null) {
            account.setStatus(Boolean.TRUE);
        }
        account.setAccountNumber(generateAccountNumber());

        Account saved = repository.save(account);
        log.info("Account created: {} for customer {}", saved.getAccountNumber(), saved.getCustomerId());
        return saved;
    }

    public Account findByAccountNumber(String accountNumber) {
        return repository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public Page<Account> findAll(Integer page, Integer size, String customerId) {
        Pageable pageable = PageRequestFactory.of(page, size, Sort.by("createdAt").descending());
        return repository.findAll(customerId, pageable);
    }

    /**
     * PUT: reemplazo total de los campos que el contrato deja editar.
     *
     * Los saldos NO se tocan aqui, y no es un olvido: son consecuencia de los
     * movimientos. Si el PUT pudiera cambiarlos, se podria alterar dinero sin
     * dejar rastro en la tabla movement. Por eso AccountUpdate ni los expone.
     */
    @Transactional
    public Account update(String accountNumber, Account changes) {
        Account existing = findByAccountNumber(accountNumber);

        if (changes.getCustomerId() != null && !changes.getCustomerId().equals(existing.getCustomerId())) {
            requireExistingCustomer(changes.getCustomerId());
            existing.setCustomerId(changes.getCustomerId());
        }
        existing.setAccountType(changes.getAccountType());
        if (changes.getStatus() != null) {
            existing.setStatus(changes.getStatus());
        }

        Account updated = repository.save(existing);
        log.info("Account updated: {}", updated.getAccountNumber());
        return updated;
    }

    /**
     * PATCH: solo se aplican los campos presentes. Recibe los valores sueltos y
     * no un DTO del contrato, porque esta capa no puede importar interfaces/rest.
     */
    @Transactional
    public Account patch(String accountNumber, AccountType accountType, Boolean status) {
        Account existing = findByAccountNumber(accountNumber);

        if (accountType != null) {
            existing.setAccountType(accountType);
        }
        if (status != null) {
            existing.setStatus(status);
        }

        Account patched = repository.save(existing);
        log.info("Account patched: {}", patched.getAccountNumber());
        return patched;
    }

    /** Con saldo distinto de cero el borrado se rechaza con 409, no con 400. */
    @Transactional
    public void delete(String accountNumber) {
        Account account = findByAccountNumber(accountNumber);
        if (account.hasBalance()) {
            throw new AccountBalanceNotZeroException(accountNumber);
        }
        repository.deleteByAccountNumber(accountNumber);
        log.info("Account deleted: {}", accountNumber);
    }

    private void requireExistingCustomer(String customerId) {
        customerLookup.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    /**
     * Numero de 6 digitos como el "478758" del contrato.
     *
     * SecureRandom y no Random: con Random la secuencia es predecible a partir de
     * unos pocos numeros observados, y esto es un identificador de cuenta bancaria.
     *
     * El formato %06d conserva los ceros a la izquierda: 4787 debe ser "004787".
     */
    private String generateAccountNumber() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = String.format("%06d", RANDOM.nextInt(ACCOUNT_NUMBER_BOUND));
            if (!repository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        // Fallar aqui es mejor que un bucle infinito: significa que el espacio de
        // numeros esta practicamente lleno y hay que ampliarlo.
        throw new IllegalStateException(
                "Could not generate a free account number after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }
}
