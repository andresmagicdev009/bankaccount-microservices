package com.application.service.infrastructure.client.customer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PASO 4.1 - Forma del JSON que devuelve el microservicio de clientes.
 *
 * No es el modelo de dominio ni un DTO del contrato propio: es la forma del dato
 * ajeno, y por eso vive en infrastructure.
 *
 * ignoreUnknown: si el otro equipo agrega campos, la deserializacion no se rompe.
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
