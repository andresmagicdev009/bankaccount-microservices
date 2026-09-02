package com.application.service.infrastructure.client.customer.dto;

/**
 * PASO 4.1 - Forma del JSON que devuelve el microservicio de clientes.
 *
 * Ojo: esto NO es el modelo de dominio ni un DTO del contrato propio. Es la
 * forma del dato ajeno, y por eso vive en infrastructure.
 *
 * TODO: campos id, name, identification (String) + @Getter @Setter @NoArgsConstructor
 *       y anota la clase con @JsonIgnoreProperties(ignoreUnknown = true), asi si
 *       el otro equipo agrega campos no se te rompe la deserializacion.
 */
public class CustomerResponse {

}
