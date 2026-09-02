package com.application.service.domain.customer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PASO 1.5 - Vista de solo lectura del cliente que vive en el microservicio de clientes.
 *
 * Este servicio NUNCA persiste esto: lo pide por REST para (a) validar que el
 * cliente existe al crear una cuenta y (b) llenar la cabecera del reporte.
 *
 * TODO: campos customerId (String), name (String), identification (String)
 *       + Lombok (@Getter @Builder @NoArgsConstructor @AllArgsConstructor).
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
