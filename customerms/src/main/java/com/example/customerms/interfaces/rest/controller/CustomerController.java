package com.example.customerms.interfaces.rest.controller;

import com.example.customerms.application.customer.service.CustomerService;
import com.example.customerms.domain.customer.entity.Customer;
import com.example.customerms.domain.customer.exception.CustomerNotFoundException;
import com.example.customerms.interfaces.rest.api.CustomersApi;
import com.example.customerms.interfaces.rest.dto.CustomerCreateDto;
import com.example.customerms.interfaces.rest.dto.CustomerDto;
import com.example.customerms.interfaces.rest.dto.CustomerPageDto;
import com.example.customerms.interfaces.rest.dto.CustomerPatchDto;
import com.example.customerms.interfaces.rest.dto.CustomerUpdateDto;
import com.example.customerms.interfaces.rest.mapper.CustomerMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Routes and status codes come from CustomersApi, generated from the OpenAPI
 * contract. Declare `implements CustomersApi` once the service is in place.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerController implements CustomersApi {

    private final CustomerService service;
    private final CustomerMapper mapper;
    private final Scheduler jdbcScheduler;

    // TODO F1: implements CustomersApi -> createCustomer, getCustomer,
    // listCustomers, updateCustomer, patchCustomer, deleteCustomer

    @Override
    public Mono<ResponseEntity<CustomerDto>> createCustomer(Mono<CustomerCreateDto> customerCreateDto,
            ServerWebExchange exchange) {
        return customerCreateDto.map(mapper::toDomain)
                .flatMap(customer -> Mono.fromCallable(() -> service.create(customer)).subscribeOn(jdbcScheduler))
                .map(mapper::toDto)
                .map(dto -> ResponseEntity
                        .created(URI.create("/api/v1/customers/" + dto.getId()))
                        .body(dto));
    }

    @Override
    public Mono<ResponseEntity<CustomerDto>> getCustomer(UUID customerId, ServerWebExchange exchange) {
        // TODO 1: convert the UUID from the path to the String the domain uses ->
        // customerId.toString()

        String id = customerId.toString();

        // TODO 2: wrap the blocking call in Mono.fromCallable(() ->
        // service.findById(id))
        // and move it off the event loop with .subscribeOn(jdbcScheduler), like
        // createCustomer does
        // and move it off the event loop with .subscribeOn(jdbcScheduler), like
        // createCustomer does
        // TODO 3: map the returned Customer to the contract type with mapper::toDto
        // TODO 4: wrap it in a 200 response -> ResponseEntity::ok
        // TODO 5: decide where the 404 comes from. CustomerService.findById throws
        // CustomerNotFoundException; it needs a @RestControllerAdvice that turns it
        // into
        // the ErrorResponse the contract declares under components/responses/NotFound.
        //
        return Mono.fromCallable(() -> service.findById(id))
                .subscribeOn(jdbcScheduler)
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteCustomer(UUID customerId, ServerWebExchange exchange) {

        String id = customerId.toString();

        return Mono.fromRunnable(() -> service.delete(id))
                .subscribeOn(jdbcScheduler)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<CustomerPageDto>> listCustomers(Integer page, Integer size, Boolean status,
            ServerWebExchange exchange) {
        // This one needs work below the controller before it can be wired up:
        //
        // TODO 1: CustomerRepositoryPort only exposes findAll() with no pagination and
        // no status
        // filter. Add a paged read to the port, e.g.
        // Page<Customer> findAll(Boolean status, Pageable pageable), and implement it
        // in
        // CustomerRepositoryAdapter over JpaCustomerRepository (findAll(Pageable) is
        // inherited; the status filter needs findByStatus(Boolean, Pageable)).
        // TODO 2: add CustomerService.findAll(page, size, status) that builds the
        // PageRequest.of(page, size) and delegates to the port. Keep it blocking, like
        // create/findById.
        // TODO 3: the generated params are nullable even though the contract declares
        // defaults
        // (page=0, size=20). Decide whether to default them here or let the generator
        // do
        // it, and clamp size to the contract range (1..100) so a caller cannot ask for
        // the whole table.
        // TODO 4: add CustomerMapper.toPageDto(Page<Customer>) filling content, page,
        // size,
        // totalElements (int64) and totalPages - all five are required by CustomerPage.
        // TODO 5: same reactive shape as createCustomer: Mono.fromCallable(...)
        // .subscribeOn(jdbcScheduler).map(mapper::toPageDto).map(ResponseEntity::ok)
        // TODO 6: the contract declares a 400 for invalid paging values; that needs the
        // same
        // @RestControllerAdvice as the 404 on getCustomer.
        return Mono.error(new UnsupportedOperationException("Not implemented yet"));
    }

    // Partially updated customer, only the fields that are present in the request
    // will be updated
    @Override
public Mono<ResponseEntity<CustomerDto>> patchCustomer(UUID customerId, Mono<CustomerPatchDto> customerPatchDto, ServerWebExchange exchange) {
    String id = customerId.toString();

    return customerPatchDto
            .flatMap(patchDto -> Mono.fromCallable(() -> service.patch(id, patchDto))
                    .subscribeOn(jdbcScheduler))
            .map(mapper::toDto)
            .map(ResponseEntity::ok); // Devolver 200 OK con el cliente actualizado
}

    // Update a customer
    @Override
    public Mono<ResponseEntity<CustomerDto>> updateCustomer(UUID customerId, Mono<CustomerUpdateDto> customerUpdateDto,
            ServerWebExchange exchange) {
        String id = customerId.toString();

        return customerUpdateDto
                .map(mapper::toDomainUpdate) // Convertir CustomerUpdateDto a Customer
                .flatMap(customer -> Mono.fromCallable(() -> service.update(id, customer))
                        .subscribeOn(jdbcScheduler))
                .map(mapper::toDto)
                .map(ResponseEntity::ok); // Devolver 200 OK con el cliente actualizado
    }

}
