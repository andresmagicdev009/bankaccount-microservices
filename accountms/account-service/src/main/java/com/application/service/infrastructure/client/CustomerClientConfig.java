package com.application.service.infrastructure.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

/**
 * WebClient pointing at the customer microservice.
 *
 * The base URL comes from configuration (customers.service.url), never
 * hardcoded: locally it resolves to localhost:8081 and inside Docker to the
 * name of the service.
 */
@Configuration
public class CustomerClientConfig {

    /**
     * The timeouts are not optional: without them, a hung customer microservice
     * hangs this one too, with the scheduler threads busy waiting forever.
     */
    @Bean
    public WebClient customerWebClient(
            @Value("${customers.service.url}") String baseUrl,
            @Value("${customers.service.timeout-ms}") int timeoutMs) {

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
