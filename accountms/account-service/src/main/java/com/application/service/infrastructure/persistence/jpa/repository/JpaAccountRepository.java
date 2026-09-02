package com.application.service.infrastructure.persistence.jpa.repository;

/**
 * PASO 2.3 - Repositorio Spring Data de cuentas.
 *
 * TODO: @Repository
 *       public interface JpaAccountRepository extends JpaRepository<AccountEntity, String>
 *       y declara los derived queries:
 *         List<AccountEntity> findByCustomerId(String customerId);
 *         Page<AccountEntity> findByCustomerId(String customerId, Pageable pageable);
 *       (el resto -findById, save, existsById, deleteById- ya lo da JpaRepository)
 */
public interface JpaAccountRepository {

}
