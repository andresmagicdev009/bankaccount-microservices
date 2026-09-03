package com.application.service.interfaces.rest.controller;

import java.net.URI;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * PASO 7.2 - Controller de movimientos.
 *
 * Mismo patron que AccountController: el borde reactivo envuelve el trabajo
 * bloqueante y las reglas F2/F3 quedan enteras en MovementService.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MovementController implements MovementsApi {

    private final MovementService service;
    private final MovementMapper mapper;
    private final Scheduler jdbcScheduler;

    @Override
    public Mono<ResponseEntity<MovementDto>> createMovement(Mono<MovementCreateDto> movementCreateDto,
            ServerWebExchange exchange) {
        return movementCreateDto
                .map(mapper::toDomain)
                .flatMap(movement -> Mono.fromCallable(() -> service.create(movement))
                        .subscribeOn(jdbcScheduler))
                .map(mapper::toDto)
                .map(dto -> ResponseEntity
                        .created(URI.create("/api/v1/movements/" + dto.getMovementId()))
                        .body(dto));
    }

    @Override
    public Mono<ResponseEntity<MovementDto>> getMovement(UUID movementId, ServerWebExchange exchange) {
        String id = movementId.toString();

        return Mono.fromCallable(() -> service.findById(id))
                .subscribeOn(jdbcScheduler)
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<MovementPageDto>> listMovements(Integer page, Integer size, String accountNumber,
            UUID customerId, LocalDate startDate, LocalDate endDate, ServerWebExchange exchange) {
        String customer = (customerId == null) ? null : customerId.toString();

        return Mono.fromCallable(
                () -> service.findAll(page, size, accountNumber, customer, startDate, endDate))
                .subscribeOn(jdbcScheduler)
                .map(mapper::toPageDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<MovementDto>> updateMovement(UUID movementId,
            Mono<MovementUpdateDto> movementUpdateDto, ServerWebExchange exchange) {
        String id = movementId.toString();

        return movementUpdateDto
                .flatMap(dto -> Mono
                        .fromCallable(() -> service.update(id,
                                mapper.toDomainType(dto.getMovementType()),
                                mapper.toAmount(dto.getValue())))
                        .subscribeOn(jdbcScheduler))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    /** Los null del patch los resuelve el servicio conservando el valor actual. */
    @Override
    public Mono<ResponseEntity<MovementDto>> patchMovement(UUID movementId,
            Mono<MovementPatchDto> movementPatchDto, ServerWebExchange exchange) {
        String id = movementId.toString();

        return movementPatchDto
                .flatMap(dto -> Mono
                        .fromCallable(() -> service.patch(id,
                                mapper.toDomainType(dto.getMovementType()),
                                mapper.toAmount(dto.getValue())))
                        .subscribeOn(jdbcScheduler))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * Borrar un movimiento no es solo quitar una fila: el servicio revierte su
     * efecto sobre el saldo de la cuenta antes de borrarlo.
     */
    @Override
    public Mono<ResponseEntity<Void>> deleteMovement(UUID movementId, ServerWebExchange exchange) {
        String id = movementId.toString();

        return Mono.<Void>fromRunnable(() -> service.delete(id))
                .subscribeOn(jdbcScheduler)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
