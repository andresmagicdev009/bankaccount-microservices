package com.application.service.infrastructure.persistence.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.application.service.infrastructure.persistence.jpa.entity.AccountEntity;

import jakarta.persistence.LockModeType;

/**
 * PASO 2.3 - Repositorio Spring Data de cuentas.
 *
 * findById, save, existsById y deleteById ya los da JpaRepository; aqui solo van
 * los derived queries propios.
 */
@Repository
public interface JpaAccountRepository extends JpaRepository<AccountEntity, String> {

    /**
     * SELECT ... FOR UPDATE sobre la fila de la cuenta.
     *
     * Necesario para mover el saldo: leer, sumar y guardar sin bloqueo deja la
     * ventana clasica del lost update -dos debitos concurrentes, uno pisa al
     * otro y el saldo puede quedar negativo pese a la regla F3-. Con el bloqueo,
     * la segunda transaccion espera al COMMIT de la primera y relee el saldo ya
     * actualizado.
     *
     * Va con @Query explicito porque el @Lock no se puede colgar del findById
     * heredado de JpaRepository. EXIGE transaccion activa: fuera de una, el
     * bloqueo se soltaria al instante y no serviria de nada.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.accountNumber = :accountNumber")
    Optional<AccountEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    List<AccountEntity> findByCustomerId(String customerId);

    Page<AccountEntity> findByCustomerId(String customerId, Pageable pageable);

    @Modifying
    @Query(value = "UPDATE account_number_seq SET next_value = LAST_INSERT_ID(next_value + 1)",
            nativeQuery = true)
    void advance();

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    long currentValue();
}

