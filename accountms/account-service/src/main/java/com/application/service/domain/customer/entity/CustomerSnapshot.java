package com.application.service.domain.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Read-only view of the customer that lives in the customer microservice.
 *
 * This service NEVER persists it: it asks for it over REST to (a) validate that
 * the customer exists when an account is created and (b) fill in the report
 * header.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSnapshot {
    private String customerId;
    private String name;
    private String identification;

}
