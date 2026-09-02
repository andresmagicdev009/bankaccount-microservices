package com.application.service.infrastructure.persistence.jpa.adapter;

/**
 * PASO 2.7 - Implementacion del puerto AccountRepositoryPort sobre Spring Data.
 *
 * Aqui es donde se cierra la inversion de dependencias: el dominio definio la
 * interfaz, infraestructura la implementa, Spring inyecta esta clase donde el
 * servicio pide el puerto.
 *
 * TODO 0: @Component @RequiredArgsConstructor  implements AccountRepositoryPort
 *         inyecta JpaAccountRepository, JpaMovementRepository y AccountPersistenceMapper
 *
 * TODO 1: save(Account)
 *         Cuidado con el insert vs update:
 *           - si accountNumber es null -> siempre insert (mapper.toEntity)
 *           - si existe la fila        -> recupera la entidad administrada y
 *                                         aplica updateEntity (asi no pisas createdAt)
 *           - si no existe             -> insert
 *
 * TODO 2: findByAccountNumber / existsByAccountNumber / findByCustomerId
 *         siempre mapeando Entity -> dominio antes de devolver.
 *
 * TODO 3: findAll(customerId, pageable)
 *         customerId null -> findAll(pageable); si no -> findByCustomerId(...)
 *         y luego page.map(mapper::toDomain).
 *
 * TODO 4: deleteByAccountNumber
 *         si no existe -> AccountNotFoundException.
 *         Decide que hacer con los movimientos: borrarlos antes, o dejar que lo
 *         haga el ON DELETE CASCADE de la migracion. Elige UNA y se consistente.
 */
public class AccountRepositoryAdapter {

}
