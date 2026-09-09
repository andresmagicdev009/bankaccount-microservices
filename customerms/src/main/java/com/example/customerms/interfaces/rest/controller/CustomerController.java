package com.example.customerms.interfaces.rest.controller;

import com.example.customerms.application.customer.service.CustomerService;
import com.example.customerms.interfaces.rest.api.CustomersApi;
import com.example.customerms.interfaces.rest.dto.CustomerCreateDto;
import com.example.customerms.interfaces.rest.dto.CustomerDto;
import com.example.customerms.interfaces.rest.dto.CustomerPageDto;
import com.example.customerms.interfaces.rest.dto.CustomerPatchDto;
import com.example.customerms.interfaces.rest.dto.CustomerUpdateDto;
import com.example.customerms.interfaces.rest.helpers.BlockingBridge;
import com.example.customerms.interfaces.rest.mapper.CustomerMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Routes and status codes come from CustomersApi, generated from the OpenAPI
 * contract.
 *
 * The controller knows nothing about the scheduler: every blocking piece of
 * work (JPA) goes through BlockingBridge, which takes it off the WebFlux event
 * loop.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class CustomerController implements CustomersApi {

    private final CustomerService service;
    private final CustomerMapper mapper;
    private final BlockingBridge blocking;

    @Override
    public Mono<ResponseEntity<CustomerDto>> postCustomer(Mono<CustomerCreateDto> customerCreateDto,
            ServerWebExchange exchange) {
        log.debug("POST /customers");
        return customerCreateDto.map(mapper::toDomain)
                .flatMap(customer -> blocking.call(() -> service.create(customer)))
                .map(mapper::toDto)
                .map(dto -> ResponseEntity
                        .created(URI.create("/api/v1/customers/" + dto.getId()))
                        .body(dto));
    }

    @Override
    public Mono<ResponseEntity<CustomerDto>> getCustomer(UUID customerId, ServerWebExchange exchange) {
        String id = customerId.toString();

        return blocking.call(() -> service.findById(id))
                .map(mapper::toDto)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteCustomer(UUID customerId, ServerWebExchange exchange) {
        String id = customerId.toString();
        log.info("DELETE /customers/{}", id);

        return blocking.run(() -> service.delete(id))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<CustomerPageDto>> getCustomers(Integer page, Integer size, Boolean status,
            ServerWebExchange exchange) {
        return blocking.call(() -> service.findAll(page, size, status))
                .map(mapper::toPageDto)
                .map(ResponseEntity::ok);
    }

    // Partially updated customer, only the fields that are present in the request
    // will be updated
    @Override
    public Mono<ResponseEntity<CustomerDto>> patchCustomer(UUID customerId, Mono<CustomerPatchDto> customerPatchDto,
            ServerWebExchange exchange) {
        String id = customerId.toString();

        return customerPatchDto
                .flatMap(patchDto -> blocking.call(() -> service.patch(id, patchDto)))
                .map(mapper::toDto)
                .map(ResponseEntity::ok); // 200 OK with the updated customer
    }

    // Update a customer
    @Override
    public Mono<ResponseEntity<CustomerDto>> putCustomer(UUID customerId, Mono<CustomerUpdateDto> customerUpdateDto,
            ServerWebExchange exchange) {
        String id = customerId.toString();

        return customerUpdateDto
                .map(mapper::toDomainUpdate) // Turn the CustomerUpdateDto into a Customer
                .flatMap(customer -> blocking.call(() -> service.update(id, customer)))
                .map(mapper::toDto)
                .map(ResponseEntity::ok); // 200 OK with the updated customer
    }
}
