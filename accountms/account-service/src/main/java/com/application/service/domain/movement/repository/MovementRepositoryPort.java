package com.application.service.domain.movement.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.application.service.domain.movement.entity.Movement;

/**
 * PASO 1.7 - Puerto de salida hacia la persistencia de movimientos.
 *
 * TODO: declara
 * Movement save(Movement movement);
 * Optional<Movement> findById(String movementId);
 * Page<Movement> findAll(String accountNumber, List<String> accountNumbers,
 * LocalDateTime from, LocalDateTime to, Pageable pageable);
 * -> los 4 filtros del GET /movements son opcionales; null = sin filtro.
 * accountNumbers sirve para filtrar por cliente (sus cuentas).
 * List<Movement> findByAccountAndRange(String accountNumber,
 * LocalDateTime from, LocalDateTime to);
 * -> para el reporte, ordenado por fecha ascendente.
 * void deleteById(String movementId);
 */
public interface MovementRepositoryPort {
    Movement save(Movement movement);

    Optional<Movement> findById(String movementId);

    Page<Movement> findAll(String accountNumber, List<String> accountNumbers,
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<Movement> findByAccountAndRange(String accountNumber,
            LocalDateTime from, LocalDateTime to);

    /**
     * Saldo resultante del ultimo movimiento de la cuenta, o vacio si todavia no
     * tiene ninguno -en ese caso el saldo disponible es el saldo inicial-.
     *
     * Esta es la unica fuente del saldo disponible: el enunciado lo modela como
     * el campo "saldo" del movimiento, no como una columna de account.
     */
    Optional<BigDecimal> findLatestBalance(String accountNumber);

    /**
     * Ultimo movimiento de la cuenta, o vacio si todavia no tiene ninguno.
     *
     * Lo usa MovementService para permitir editar o borrar solo el ultimo:
     * findLatestBalance devuelve el saldo pero no el id, y para esa regla hace
     * falta el id.
     */
    Optional<Movement> findLatest(String accountNumber);

    void deleteById(String movementId);
}
