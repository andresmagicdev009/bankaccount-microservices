package com.application.service.infrastructure.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * PASO 3.2 - WebClient apuntando al microservicio de clientes.
 *
 * La URL base entra por configuracion (customers.service.url), nunca a mano:
 * en local resuelve a localhost:8081 y dentro de Docker al nombre del servicio.
 */
@Configuration
public class CustomerClientConfig {

    /**
     * Los timeouts no son opcionales: sin ellos, un microservicio de clientes
     * colgado deja colgado tambien a este, con los hilos del scheduler ocupados
     * esperando para siempre.
     */
    @Bean
    public WebClient customerWebClient(
            @Value("${customers.service.url}") String baseUrl,
            @Value("${customers.service.timeout-ms:3000}") int timeoutMs) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
