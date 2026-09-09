package com.application.service.infrastructure.customer;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.application.service.domain.customer.entity.CustomerSnapshot;
import com.application.service.domain.customer.exception.CustomerServiceUnavailableException;
import com.application.service.domain.customer.port.CustomerLookupPort;
import com.application.service.domain.customer.dto.CustomerResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter implementing CustomerLookupPort with WebClient.
 *
 * Its reason to exist is translating failures: upwards, empty means 404 and an
 * exception means 502.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerLookupAdapter implements CustomerLookupPort {

    private final WebClient customerWebClient;

    /**
     * The .block() is acceptable because the use cases run on the jdbcScheduler,
     * never on the event loop. If some day this gets called from the event
     * loop, the whole path has to become reactive.
     */
    @Override
    public Optional<CustomerSnapshot> findById(String customerId) {
        try {
            CustomerResponse response = customerWebClient.get()
                    .uri("/customers/{customerId}", customerId)
                    .retrieve()
                    .bodyToMono(com.application.service.domain.customer.dto.CustomerResponse.class)
                    .block();

            return Optional.ofNullable(response).map(this::toSnapshot);

        } catch (WebClientResponseException.NotFound ex) {
            // The customer does not exist: this is not a failure of the remote service.
            log.debug("Customer {} not found in customer service", customerId);
            return Optional.empty();

        } catch (RuntimeException ex) {
            // Timeout, refused connection, dead DNS or 5xx: upstream fault -> 502.
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
