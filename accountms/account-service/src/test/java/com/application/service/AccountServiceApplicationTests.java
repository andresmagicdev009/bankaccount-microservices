package com.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
// El contenedor de pruebas crea el esquema con schemas/BaseDatos.sql, asi que
// Flyway no tiene nada que aplicar: encendido intentaria correr V1 sobre tablas
// que ya existen.
@TestPropertySource(properties = "spring.flyway.enabled=false")
class AccountServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
