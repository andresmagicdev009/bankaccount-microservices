package com.application.service.domain.customer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shape of the JSON returned by the customer microservice.
 *
 * It is neither the domain model nor a DTO of our own contract: it is the shape
 * of data owned by somebody else.
 *
 * ignoreUnknown: if the other team adds fields, deserialization does not break.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerResponse {

    private String id;
    private String name;
    private String identification;
}
