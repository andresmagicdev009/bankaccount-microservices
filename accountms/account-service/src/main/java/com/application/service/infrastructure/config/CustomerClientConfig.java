package com.application.service.infrastructure.config;

/**
 * PASO 3.2 - WebClient apuntando al microservicio de clientes.
 *
 * La URL base NO se escribe a mano: entra por configuracion. En
 * application.properties ya esta la propiedad customers.service.url, que a su
 * vez lee la variable de entorno CUSTOMERS_SERVICE_URL. Dentro de Docker
 * resuelve por el nombre del servicio (ms-customers).
 *
 * TODO: @Configuration + @Bean WebClient customerWebClient(...)
 *       a) inyecta con @Value("${customers.service.url}") y
 *          @Value("${customers.service.timeout-ms:3000}")
 *       b) configura timeouts (importante: sin timeout, un cliente colgado
 *          cuelga tambien a este servicio):
 *          HttpClient.create()
 *              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
 *              .responseTimeout(Duration.ofMillis(timeoutMs))
 *       c) WebClient.builder().baseUrl(...).clientConnector(new ReactorClientHttpConnector(httpClient)).build()
 */
public class CustomerClientConfig {

}
