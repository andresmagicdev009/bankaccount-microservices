package com.example.customerms;

import org.junit.jupiter.api.Test;

/**
 * Test de humo. El metodo va vacio a proposito: lo que se prueba es que el
 * contexto arranque, y eso ocurre antes de entrar al cuerpo.
 *
 * Detecta arranques rotos -bean que no se construye, property mal escrita,
 * Flyway que no aplica, entidad desincronizada del esquema- con un solo fallo
 * legible en vez de seis tests funcionales cayendo a la vez.
 *
 * Hereda de AbstractIntegrationTest para compartir contexto (y contenedor) con
 * el resto de tests de integracion.
 */
class CustomermsApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
