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
 * PASO 6.2 - Frontera HTTP <-> dominio para movimientos.
 */
@Component
public class MovementMapper {

    // ------------------------------------------------------------ entrada

    /**
     * movementId, date y balance no se mapean: son readOnly en el contrato y los
     * asigna MovementService al aplicar el movimiento sobre el saldo.
     */
    public Movement toDomain(MovementCreateDto dto) {
        return Movement.builder()
                .accountNumber(dto.getAccountNumber())
                .movementType(toDomainType(dto.getMovementType()))
                .value(DtoTypes.toAmount(dto.getValue()))
                .build();
    }

    /**
     * PUT: solo viajan movementType y value. accountNumber, date y balance no
     * estan en el contrato de update -mover un movimiento de cuenta o reescribir
     * su fecha descuadraria el saldo historico-, asi que el Movement sale con
     * esos campos en null y MovementService conserva los del existente.
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

    /** En PATCH el null significa "conserva el valor actual". */
    public MovementType toDomainType(MovementPatchDto.MovementTypeEnum movementType) {
        return DtoTypes.toDomainEnum(MovementType.class, movementType);
    }

    public BigDecimal toAmount(Double value) {
        return DtoTypes.toAmount(value);
    }

    // ------------------------------------------------------------- salida

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
