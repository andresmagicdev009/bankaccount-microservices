package com.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
// The test container creates the schema from schemas/BaseDatos.sql, so Flyway
// has nothing to apply: switched on it would try to run V1 over tables that
// already exist.
@TestPropertySource(properties = "spring.flyway.enabled=false")
class AccountServiceApplicationIT {

	@Test
	void contextLoads() {
	}

}
