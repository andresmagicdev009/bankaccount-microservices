package com.example.customerms.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class SchedulerConfig {
    @Bean 
    public Scheduler jdbcScheduler(
        @Value("${spring.datasource.hikari.maximum-pool-size:10}") int poolSize
    ) {
        return Schedulers.newBoundedElastic(poolSize, 10_000, "jdbc");
    }
}
