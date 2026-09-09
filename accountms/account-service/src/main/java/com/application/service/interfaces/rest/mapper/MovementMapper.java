package com.application.service.interfaces.rest.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.application.service.domain.movement.entity.Movement;
import com.application.service.domain.movement.entity.MovementType;
import com.application.service.interfaces.rest.dto.MovementCreateDto;
import com.application.service.interfaces.rest.dto.MovementDto;
import com.application.service.interfaces.rest.dto.MovementPageDto;
import com.application.service.interfaces.rest.dto.MovementPatchDto;
import com.application.service.interfaces.rest.dto.MovementUpdateDto;

/**
 * HTTP <-> domain boundary for movements.
 */
@Component
public class MovementMapper {

    // -------------------------------------------------------------- input

    /**
     * movementId, date and balance are not mapped: they are readOnly in the
     * contract and assigned by MovementService while applying the movement to
     * the balance.
     */
    public Movement toDomain(MovementCreateDto dto) {
        return Movement.builder()
                .accountNumber(dto.getAccountNumber())
                .movementType(toDomainType(dto.getMovementType()))
                .value(DtoTypes.toAmount(dto.getValue()))
                .build();
    }

    /**
     * PUT: only movementType and value travel. accountNumber, date and balance
     * are not in the update contract -moving a movement between accounts or
     * rewriting its date would unbalance the historical balance-, so the
     * Movement comes out with those fields null and MovementService keeps the
     * ones of the existing row.
     */
    public Movement toDomain(MovementUpdateDto dto) {
        return Movement.builder()
                .movementType(toDomainType(dto.getMovementType()))
                .value(DtoTypes.toAmount(dto.getValue()))
                .build();
    }

    public MovementType toDomainType(MovementCreateDto.MovementTypeEnum movementType) {
        return DtoTypes.toDomainEnum(MovementType.class, movementType);
    }

    public MovementType toDomainType(MovementUpdateDto.MovementTypeEnum movementType) {
        return DtoTypes.toDomainEnum(MovementType.class, movementType);
    }

    /** In a PATCH, null means "keep the current value". */
    public MovementType toDomainType(MovementPatchDto.MovementTypeEnum movementType) {
        return DtoTypes.toDomainEnum(MovementType.class, movementType);
    }

    public BigDecimal toAmount(Double value) {
        return DtoTypes.toAmount(value);
    }

    // ------------------------------------------------------------- output

    public MovementDto toDto(Movement movement) {
        return new MovementDto()
                .movementId(DtoTypes.toUuid(movement.getMovementId()))
                .date(DtoTypes.toContractDate(movement.getDate()))
                .movementType(toContractType(movement.getMovementType()))
                .value(DtoTypes.toContractAmount(movement.getValue()))
                .balance(DtoTypes.toContractAmount(movement.getBalance()))
                .accountNumber(movement.getAccountNumber());
    }

    public MovementPageDto toPageDto(Page<Movement> page) {
        List<MovementDto> content = page.getContent().stream()
                .map(this::toDto)
                .toList();

        return new MovementPageDto()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements((int) page.getTotalElements())
                .totalPages(page.getTotalPages());
    }

    private MovementDto.MovementTypeEnum toContractType(MovementType movementType) {
        return movementType == null ? null : MovementDto.MovementTypeEnum.fromValue(movementType.name());
    }
}
