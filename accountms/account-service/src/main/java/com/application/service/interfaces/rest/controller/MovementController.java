package com.application.service.interfaces.rest.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.Callable;

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
 *
 * Las rutas y los codigos de estado vienen de MovementsApi, generada del
 * contrato: aqui no hay ni un @PostMapping ni un @RequestMapping.
 *
 * No hay try/catch. InsufficientBalanceException sube hasta
 * GlobalExceptionHandler, que la traduce a 422 con "Saldo no disponible".
 * Atraparla aqui romperia la regla F3.
 *
 * Conversion recurrente: el contrato declara movementId y customerId como UUID
 * y el dominio los guarda como String. La traduccion se hace en este borde,
 * nunca en el servicio.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MovementController implements MovementsApi {

    private final MovementService movementService;
    private final MovementMapper movementMapper;
    private final Scheduler jdbcScheduler;

    /**
     * POST /movements -> 201 con cabecera Location.
     *
     * El body llega como Mono porque el contrato es reactivo: hasta que no se
     * suscribe no hay DTO. De ahi el map para traducirlo y el flatMap para
     * encadenar el trabajo bloqueante, que ya devuelve otro Mono.
     */
    @Override
    public Mono<ResponseEntity<MovementDto>> createMovement(Mono<MovementCreateDto> movementCreateDto,
            ServerWebExchange exchange) {
        return movementCreateDto
                .map(movementMapper::toDomain)
                .flatMap(movement -> blocking(() -> movementService.create(movement)))
                .map(movementMapper::toDto)
                .map(dto -> ResponseEntity
                        .created(location(exchange, dto.getMovementId().toString()))
                        .body(dto));
    }

    @Override
    public Mono<ResponseEntity<MovementDto>> getMovement(UUID movementId, ServerWebExchange exchange) {
        return blocking(() -> movementService.get(movementId.toString()))
                .map(movementMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * GET /movements?page&size&accountNumber&customerId&startDate&endDate.
     *
     * Es el listado del punto 5 del enunciado: por fechas y por usuario.
     *
     * Las fechas se pasan tal cual como LocalDate: el servicio las expande a
     * LocalDateTime -toFrom/toTo-. Convertirlas aqui duplicaria esa regla.
     */
    @Override
    public Mono<ResponseEntity<MovementPageDto>> listMovements(Integer page, Integer size, String accountNumber,
            UUID customerId, LocalDate startDate, LocalDate endDate, ServerWebExchange exchange) {
        String customer = (customerId == null) ? null : customerId.toString();

        return blocking(() -> movementService.list(accountNumber, customer, startDate, endDate, page, size))
                .map(movementMapper::toPageDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<MovementDto>> updateMovement(UUID movementId, Mono<MovementUpdateDto> movementUpdateDto,
            ServerWebExchange exchange) {
        return movementUpdateDto
                .map(movementMapper::toDomain)
                .flatMap(changes -> blocking(() -> movementService.update(movementId.toString(), changes)))
                .map(movementMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * El PATCH no pasa por un Movement: sus campos nulos significan "no cambies
     * esto", y un Movement a medio llenar no sabria distinguir eso de un "ponlo
     * a null". Por eso el servicio recibe los dos campos sueltos.
     */
    @Override
    public Mono<ResponseEntity<MovementDto>> patchMovement(UUID movementId, Mono<MovementPatchDto> movementPatchDto,
            ServerWebExchange exchange) {
        return movementPatchDto
                .flatMap(dto -> blocking(() -> movementService.patch(movementId.toString(),
                        movementMapper.toDomainType(dto.getMovementType()),
                        movementMapper.toAmount(dto.getValue()))))
                .map(movementMapper::toDto)
                .map(ResponseEntity::ok);
    }

    /**
     * delete no devuelve nada, pero Mono.fromCallable no admite null: se
     * devuelve un booleano de relleno que solo sirve para disparar el 204.
     */
    @Override
    public Mono<ResponseEntity<Void>> deleteMovement(UUID movementId, ServerWebExchange exchange) {
        return blocking(() -> {
            movementService.delete(movementId.toString());
            return true;
        }).map(deleted -> ResponseEntity.noContent().build());
    }

    // ----------------------------------------------------------------- HELPERS

    /**
     * Unico sitio que empuja el trabajo bloqueante fuera del event loop.
     *
     * fromCallable y no just(...): con just, la llamada se ejecutaria al armar
     * el pipeline -en el event loop- y subscribeOn no serviria de nada.
     */
    private <T> Mono<T> blocking(Callable<T> work) {
        return Mono.fromCallable(work).subscribeOn(jdbcScheduler);
    }

    /**
     * Location del 201, construida sobre la ruta de la peticion para que
     * incluya el base-path (/api/v1) sin repetirlo aqui.
     */
    private URI location(ServerWebExchange exchange, String movementId) {
        return URI.create(exchange.getRequest().getPath().value() + "/" + movementId);
    }
}
