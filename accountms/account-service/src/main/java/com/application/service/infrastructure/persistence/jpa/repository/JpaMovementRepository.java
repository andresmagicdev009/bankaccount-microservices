package com.application.service.infrastructure.persistence.jpa.repository;

/**
 * PASO 2.4 - Repositorio Spring Data de movimientos.
 *
 * TODO: @Repository
 *       extends JpaRepository<MovementEntity, String>, JpaSpecificationExecutor<MovementEntity>
 *
 *       Por que ademas JpaSpecificationExecutor: el GET /movements tiene 4 filtros
 *       opcionales. Una @Query con parametros nulos ("... :param IS NULL OR ...")
 *       se vuelve fragil, sobre todo con listas y fechas. Con Specification armas
 *       los predicados solo cuando el filtro viene.
 *
 *       Declara ademas:
 *         List<MovementEntity> findByAccountNumberAndDateBetweenOrderByDateAsc(
 *                 String accountNumber, LocalDateTime from, LocalDateTime to);
 *         void deleteByAccountNumber(String accountNumber);
 *
 * TODO (opcional): crea el paquete specification/ con una clase
 *       MovementSpecifications que arme el filtro; deja este repositorio limpio.
 */
public interface JpaMovementRepository {

}
