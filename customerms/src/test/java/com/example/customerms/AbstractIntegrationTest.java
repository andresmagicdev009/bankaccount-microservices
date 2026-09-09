package com.example.customerms;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;

/**
 * Base de los tests que necesitan la aplicacion entera contra MySQL real.
 *
 * Existe por el cache de contextos de Spring: la clave de cache se arma con la
 * configuracion combinada de la clase de test, asi que dos clases con
 * anotaciones identicas comparten contexto -y, por tanto, un unico contenedor
 * MySQL, arrancado una sola vez para toda la suite. En cuanto una clase cambia
 * una property o el webEnvironment, se abre un contexto nuevo y se paga otro
 * arranque de Docker. Heredar de aqui evita que eso pase por descuido.
 *
 * Que aporta cada anotacion:
 *
 * - @Import(TestcontainersConfiguration): levanta el contenedor y @ServiceConnection
 *   apunta spring.datasource.* hacia el. Flyway aplica V1__init_schema.sql y
 *   ddl-auto=validate comprueba que las entidades cuadren con esa migracion.
 * - RANDOM_PORT: servidor Netty de verdad, no un mock. El request cruza el event
 *   loop y BlockingBridge igual que en produccion.
 * - @AutoConfigureWebTestClient: en Boot 4 el soporte de WebTestClient se movio
 *   a su propio modulo y ya no basta con RANDOM_PORT para tener el bean.
 *
 * Las subclases NO deben anotarse con @SpringBootTest ni repetir estas
 * anotaciones: cualquier diferencia rompe el contexto compartido.
 */
@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.test.webtestclient.timeout=30s")
public abstract class AbstractIntegrationTest {
}
