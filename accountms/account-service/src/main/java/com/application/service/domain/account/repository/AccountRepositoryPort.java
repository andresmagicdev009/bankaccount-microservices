package com.application.service.domain.account.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.application.service.domain.account.entity.Account;

/**
 * PASO 1.6 - Puerto de salida hacia la persistencia de cuentas.
 *
 * Por que una interfaz aqui y no usar JpaRepository directo en el servicio:
 * el dominio declara QUE necesita; infraestructura decide COMO (hoy JPA, manana
 * lo que sea). Asi la capa application no depende de Spring Data.
 *
 * TODO: declara los metodos (todos devuelven/reciben tipos de DOMINIO, nunca
 * Entity)
 * Account save(Account account);
 * Optional<Account> findByAccountNumber(String accountNumber);
 * boolean existsByAccountNumber(String accountNumber);
 * List<Account> findByCustomerId(String customerId); -> lo usa el reporte
 * Page<Account> findAll(String customerId, Pageable pageable);
 * void deleteByAccountNumber(String accountNumber);
 *
 * Nota: Page y Pageable de Spring Data si se permiten aqui (es lo que hace
 * customerms). Si quieres el dominio 100% puro, tendrias que crear tu propio
 * tipo de paginacion; para este proyecto no vale la pena.
 */
public interface AccountRepositoryPort {
    
    Account save(Account account);

    /**
     * Escritura dedicada del saldo disponible: es la unica que toca esa columna.
     *
     * No va por save(Account) a proposito. save copia todo el estado de la
     * cuenta, asi que un PUT /accounts que hubiera leido la fila antes de un
     * movimiento reescribiria el saldo viejo encima del nuevo. Separando la
     * escritura, el CRUD de cuentas ya no puede pisar el saldo ni por descuido.
     */
    void updateAvailableBalance(String accountNumber, BigDecimal availableBalance);

    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Igual que findByAccountNumber pero bloqueando la fila hasta el COMMIT.
     *
     * Lo usa todo el que vaya a mover el saldo. Solo tiene sentido dentro de una
     * transaccion: sin ella el bloqueo se libera de inmediato.
     */
    Optional<Account> findByAccountNumberForUpdate(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(String customerId);

    Page<Account> findAll(String customerId, Pageable pageable);

    void deleteByAccountNumber(String accountNumber);

    long nextAccountNumberSequenceValue();
}
