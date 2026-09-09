package com.example.customerms.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Scheduler that carries the blocking JPA work off the WebFlux event loop.
 *
 * Its size is tied to the Hikari pool: more threads than connections would only
 * queue work inside the pool, and fewer would leave connections idle.
 */
@Slf4j
@Configuration
public class SchedulerConfig {

    @Bean
    public Scheduler jdbcScheduler(
        @Value("${spring.datasource.hikari.maximum-pool-size:10}") int poolSize
    ) {
        log.info("Creating jdbc scheduler with {} threads (matching the Hikari pool size)", poolSize);
        return Schedulers.newBoundedElastic(poolSize, 10_000, "jdbc");
    }
}