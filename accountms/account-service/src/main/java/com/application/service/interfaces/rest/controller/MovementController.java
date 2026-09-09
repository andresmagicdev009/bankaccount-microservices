package com.application.service.interfaces.rest.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.application.service.application.movement.service.MovementService;
import com.application.service.interfaces.rest.api.MovementsApi;
import com.application.service.interfaces.rest.dto.MovementCreateDto;
import com.application.service.interfaces.rest.dto.MovementDto;
import com.application.service.interfaces.rest.dto.MovementPageDto;
import com.application.service.interfaces.rest.dto.MovementPatchDto;
import com.application.service.interfaces.rest.dto.MovementUpdateDto;
import com.application.service.interfaces.rest.mapper.MovementMapper;
import com.application.service.interfaces.rest.helpers.BlockingBridge;
import com.application.service.interfaces.rest.helpers.ResourceLocation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Movements controller.
 *
 * Same pattern as AccountController: the blocking work leaves the event loop
 * through BlockingBridge and rules F2/F3 stay whole inside MovementService.
 *
 * The routes and the status codes come from MovementsApi, generated from the
 * contract: there is not a single @PostMapping or @RequestMapping here.
 *
 * There is no try/catch. InsufficientBalanceException travels up to
 * GlobalExceptionHandler, which turns it into a 422 with "Saldo no disponible".
 * Catching it here would break rule F3.
 *
 * A recurring conversion: the contract declares movementId and customerId as
 * UUIDs and the domain stores them as Strings. The translation happens at this
 * edge, never in the service.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MovementController implements MovementsApi {

    private final MovementService movementService;
    private final MovementMapper movementMapper;
    private final BlockingBridge blocking;

    /**
     * POST /movements -> 201 with a Location header.
     *
     * The body arrives as a Mono because the contract is reactive: there is no
     * DTO until it is subscribed to. Hence the map to translate it and the
     * flatMap to chain the blocking work, which already returns another Mono.
     */
    @Override
    public Mono<ResponseEntity<MovementDto>> postMovement(Mono<MovementCreateDto> movementCreateDto,
            ServerWebExchange exchange) {
        return movementCreateDto
                .map(movementMapper::toDomain)
                .flatMap(movement -> blocking.call(() -> movementService.create(movement)))
                .map(movementMapper::toDto)
                .map(dto -> ResponseEntity
                        .created(ResourceLocation.of(exchange, dto.getMovementId().toString()))
                        .body(dto));
    }

    @Override
    public Mono<ResponseEntity<MovementDto>> getMovement(UUID movementId, ServerWebExchange exchange) {
        return blocking.call(() -> movementService.get(movementId.toString()))
                .map(movementMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * GET /movements?page&size&accountNumber&customerId&startDate&endDate.
     *
     * This is the listing of point 5 of the specification: by date and by
     * customer.
     *
     * The dates are passed straight through as LocalDate: the service expands
     * them to LocalDateTime -toFrom/toTo-. Converting them here would duplicate
     * that rule.
     */
    @Override
    public Mono<ResponseEntity<MovementPageDto>> getMovements(Integer page, Integer size, String accountNumber,
            UUID customerId, LocalDate startDate, LocalDate endDate, ServerWebExchange exchange) {
        String customer = (customerId == null) ? null : customerId.toString();

        return blocking.call(() -> movementService.list(accountNumber, customer, startDate, endDate, page, size))
                .map(movementMapper::toPageDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<MovementDto>> putMovement(UUID movementId, Mono<MovementUpdateDto> movementUpdateDto,
            ServerWebExchange exchange) {
        return movementUpdateDto
                .map(movementMapper::toDomain)
                .flatMap(changes -> blocking.call(() -> movementService.update(movementId.toString(), changes)))
                .map(movementMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * The PATCH does not go through a Movement: its null fields mean "do not change
     * this", and a half-filled Movement could not tell that apart from "set it to
     * null". That is why the service receives the two fields loose.
     */
    @Override
    public Mono<ResponseEntity<MovementDto>> patchMovement(UUID movementId, Mono<MovementPatchDto> movementPatchDto,
            ServerWebExchange exchange) {
        return movementPatchDto
                .flatMap(dto -> blocking.call(() -> movementService.patch(movementId.toString(),
                        movementMapper.toDomainType(dto.getMovementType()),
                        movementMapper.toAmount(dto.getValue()))))
                .map(movementMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /** delete returns nothing: the empty Mono triggers the 204. */
    @Override
    public Mono<ResponseEntity<Void>> deleteMovement(UUID movementId, ServerWebExchange exchange) {
        return blocking.run(() -> movementService.delete(movementId.toString()))
                .thenReturn(ResponseEntity.noContent().build());
    }
}
