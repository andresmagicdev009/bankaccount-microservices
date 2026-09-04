package com.application.service.infrastructure.persistence.jpa.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.repository.MovementRepositoryPort;
import com.application.service.infrastructure.persistence.jpa.entity.MovementEntity;
import com.application.service.infrastructure.persistence.jpa.mapper.MovementPersistenceMapper;
import com.application.service.infrastructure.persistence.jpa.repository.JpaMovementRepository;
import com.application.service.infrastructure.persistence.jpa.specification.MovementSpecifications;

import lombok.RequiredArgsConstructor;

/** PASO 2.8 - Implementacion de MovementRepositoryPort. */
@Component
@RequiredArgsConstructor
public class MovementRepositoryAdapter implements MovementRepositoryPort {

    private final JpaMovementRepository movementRepository;
    private final MovementPersistenceMapper mapper;

    @Override
    public Movement save(Movement movement) {
        if (movement.getMovementId() == null) {
            return mapper.toDomain(movementRepository.save(mapper.toEntity(movement)));
        }

        MovementEntity entity = movementRepository.findById(movement.getMovementId())
                .map(managed -> {
                    mapper.updateEntity(managed, movement);
                    return managed;
                })
                .orElseGet(() -> mapper.toEntity(movement));

        return mapper.toDomain(movementRepository.save(entity));
    }

    @Override
    public Optional<Movement> findById(String movementId) {
        return movementRepository.findById(movementId).map(mapper::toDomain);
    }

    @Override
    public Page<Movement> findAll(String accountNumber, List<String> accountNumbers,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return movementRepository
                .findAll(MovementSpecifications.filterBy(accountNumber, accountNumbers, from, to), pageable)
                .map(mapper::toDomain);
    }

    /** Orden ascendente por fecha: es como el reporte encadena los saldos. */
    @Override
    public List<Movement> findByAccountAndRange(String accountNumber,
            LocalDateTime from, LocalDateTime to) {
        return movementRepository
                .findByAccountNumberAndDateBetweenOrderByDateAsc(accountNumber, from, to).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<BigDecimal> findLatestBalance(String accountNumber) {
        return movementRepository
                .findFirstByAccountNumberOrderByDateDescMovementIdDesc(accountNumber)
                .map(MovementEntity::getBalance);
    }

    @Override
    public Optional<Movement> findLatest(String accountNumber) {
        return movementRepository
                .findFirstByAccountNumberOrderByDateDescMovementIdDesc(accountNumber)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteById(String movementId) {
        movementRepository.deleteById(movementId);
    }
}
