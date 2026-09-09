package com.application.service.domain.customer.port;

import java.util.Optional;

import com.application.service.domain.customer.entity.CustomerSnapshot;

/**
 * Outbound port towards the customer microservice.
 *
 * The domain does not know that WebClient or HTTP exist: it only knows that
 * "somebody" can resolve a customer by id. The implementation lives in
 * infrastructure.
 *
 * Contract of the method (it drives the HTTP codes):
 *   - unknown customer (a 404 upstream)   -> Optional.empty()
 *   - timeout / refused connection / 5xx  -> throws CustomerServiceUnavailableException
 * That distinction is what is later translated into 404 vs 502.
 */
public interface CustomerLookupPort {
    Optional<CustomerSnapshot> findById(String customerId);
}
