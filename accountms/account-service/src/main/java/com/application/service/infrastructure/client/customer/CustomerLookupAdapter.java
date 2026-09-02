package com.application.service.infrastructure.client.customer;

/**
 * PASO 4.2 - Adaptador que implementa CustomerLookupPort con WebClient.
 *
 * TODO 0: @Component @RequiredArgsConstructor @Slf4j
 *         implements CustomerLookupPort
 *         private final WebClient customerWebClient;
 *
 * TODO 1: findById(String customerId)
 *         customerWebClient.get()
 *             .uri("/customers/{customerId}", customerId)
 *             .retrieve()
 *             .bodyToMono(CustomerResponse.class)
 *             .block();
 *
 *         Por que .block() aqui es aceptable: los casos de uso se ejecutan sobre
 *         el jdbcScheduler, nunca sobre el event loop. Si algun dia llamas esto
 *         desde el event loop, hay que volverlo reactivo.
 *
 * TODO 2: traduce los fallos (esta es la razon de ser del adaptador)
 *         catch WebClientResponseException.NotFound -> return Optional.empty()
 *         catch RuntimeException                    -> lanza CustomerServiceUnavailableException
 *         Con eso, arriba: vacio = 404, excepcion = 502.
 *
 * TODO 3: metodo privado que convierta CustomerResponse -> CustomerSnapshot.
 */
public class CustomerLookupAdapter {

}
