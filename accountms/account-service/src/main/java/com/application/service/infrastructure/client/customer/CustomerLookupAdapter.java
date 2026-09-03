package com.application.service.infrastructure.client.customer;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.application.service.domain.customer.entity.CustomerSnapshot;
import com.application.service.domain.customer.exception.CustomerServiceUnavailableException;
import com.application.service.domain.customer.port.CustomerLookupPort;
import com.application.service.infrastructure.client.customer.dto.CustomerResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PASO 4.2 - Adaptador que implementa CustomerLookupPort con WebClient.
 *
 * Su razon de ser es traducir fallos: hacia arriba, vacio = 404 y excepcion = 502.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerLookupAdapter implements CustomerLookupPort {

    private final WebClient customerWebClient;

    /**
     * El .block() es aceptable porque los casos de uso corren sobre el
     * jdbcScheduler, nunca sobre el event loop. Si algun dia se llama desde el
     * event loop, hay que volver reactivo todo el camino.
     */
    @Override
    public Optional<CustomerSnapshot> findById(String customerId) {
        try {
            CustomerResponse response = customerWebClient.get()
                    .uri("/customers/{customerId}", customerId)
                    .retrieve()
                    .bodyToMono(CustomerResponse.class)
                    .block();

            return Optional.ofNullable(response).map(this::toSnapshot);

        } catch (WebClientResponseException.NotFound ex) {
            // El cliente no existe: no es una falla del servicio remoto.
            log.debug("Customer {} not found in customer service", customerId);
            return Optional.empty();

        } catch (RuntimeException ex) {
            // Timeout, conexion rechazada, DNS caido o 5xx: culpa de arriba -> 502.
            throw new CustomerServiceUnavailableException(customerId, ex);
        }
    }

    private CustomerSnapshot toSnapshot(CustomerResponse response) {
        return CustomerSnapshot.builder()
                .customerId(response.getId())
                .name(response.getName())
                .identification(response.getIdentification())
                .build();
    }
}
