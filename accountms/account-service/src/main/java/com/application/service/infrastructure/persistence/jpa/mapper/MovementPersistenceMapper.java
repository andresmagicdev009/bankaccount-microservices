package com.application.service.infrastructure.persistence.jpa.mapper;

import org.springframework.stereotype.Component;

import com.application.service.domain.movement.entity.Movement;
import com.application.service.infrastructure.persistence.jpa.entity.MovementEntity;

/** PASO 2.6 - Traductor Movement (dominio) <-> MovementEntity (JPA). */
@Component
public class MovementPersistenceMapper {

    public MovementEntity toEntity(Movement movement) {
        MovementEntity entity = new MovementEntity();
        entity.setMovementId(movement.getMovementId());
        copyState(entity, movement);
        return entity;
    }

    /**
     * El accountNumber no se copia: un movimiento no cambia de cuenta. Moverlo
     * descuadraria el saldo de las dos cuentas, asi que se fija en el insert.
     */
    public void updateEntity(MovementEntity entity, Movement movement) {
        copyState(entity, movement);
    }

    public Movement toDomain(MovementEntity entity) {
        if (entity == null) {
            return null;
        }
        return Movement.builder()
                .movementId(entity.getMovementId())
                .date(entity.getDate())
                .movementType(entity.getMovementType())
                .value(entity.getValue())
                .balance(entity.getBalance())
                .accountNumber(entity.getAccountNumber())
                .build();
    }

    private void copyState(MovementEntity entity, Movement movement) {
        entity.setDate(movement.getDate());
        entity.setMovementType(movement.getMovementType());
        entity.setValue(movement.getValue());
        entity.setBalance(movement.getBalance());
        if (entity.getAccountNumber() == null) {
            entity.setAccountNumber(movement.getAccountNumber());
        }
    }
}
