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
 * Spring Data repository of accounts.
 *
 * findById, save, existsById and deleteById already come from JpaRepository;
 * only the derived queries of our own live here.
 */
@Repository
public interface JpaAccountRepository extends JpaRepository<AccountEntity, String> {

    /**
     * SELECT ... FOR UPDATE on the row of the account.
     *
     * Needed to move the balance: read, add and save without a lock leaves the
     * classic lost-update window -two concurrent debits, one overwrites the
     * other and the balance can end up negative despite rule F3-. With the
     * lock, the second transaction waits for the COMMIT of the first and
     * re-reads the balance already updated.
     *
     * It carries an explicit @Query because @Lock cannot be attached to the
     * findById inherited from JpaRepository. It REQUIRES an active transaction:
     * outside one, the lock would be released instantly and be useless.
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

