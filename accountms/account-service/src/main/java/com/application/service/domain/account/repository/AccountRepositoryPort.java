package com.application.service.domain.account.repository;

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

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(String customerId);

    Page<Account> findAll(String customerId, Pageable pageable);

    void deleteByAccountNumber(String accountNumber);
}
