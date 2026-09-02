package com.application.service.domain.customer.port;

/**
 * PASO 1.8 - Puerto de salida hacia el microservicio de clientes.
 *
 * El dominio no sabe que existe WebClient ni HTTP: solo sabe que "alguien" le
 * puede resolver un cliente por id. La implementacion va en infrastructure.
 *
 * TODO: declara
 *       Optional<CustomerSnapshot> findById(String customerId);
 *
 *       Contrato del metodo (importante para los codigos HTTP):
 *         - cliente inexistente (404 arriba)  -> Optional.empty()
 *         - timeout / conexion rechazada / 5xx -> lanza CustomerServiceUnavailableException
 *       Esa distincion es la que despues se traduce a 404 vs 502.
 */
public interface CustomerLookupPort {

}
