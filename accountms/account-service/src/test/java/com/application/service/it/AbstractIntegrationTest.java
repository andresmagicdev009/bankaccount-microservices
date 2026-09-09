package com.application.service.it;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.mysql.MySQLContainer;

import com.application.service.TestcontainersConfiguration;
import com.application.service.interfaces.rest.dto.AccountDto;
import com.application.service.interfaces.rest.dto.MovementDto;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Base de las pruebas de integracion.
 *
 * Que se prueba de verdad aqui, y que no:
 *
 * - Servidor HTTP real (RANDOM_PORT), asi que la peticion recorre el camino
 *   completo: rutas generadas del contrato -> controller -> BlockingBridge ->
 *   servicio @Transactional -> JPA -> MariaDB, y los errores vuelven por
 *   GlobalExceptionHandler. Nada esta mockeado dentro del servicio.
 * - El esquema es schemas/BaseDatos.sql, el entregable. Flyway va apagado: si
 *   estuviera encendido intentaria aplicar V1 sobre tablas que ya existen.
 *   ddl-auto sigue en validate -lo hereda de application.properties- a
 *   proposito: si el entregable se separa de las entidades, el contexto no
 *   levanta y la suite entera falla. Ese es el aviso que queremos.
 * - El microservicio de clientes es un WireMock, no un mock de Mockito. Asi la
 *   prueba cubre tambien CustomerLookupAdapter y su WebClient: el 404 que se
 *   traduce a Optional vacio, el 5xx que sube como 502 y el timeout.
 *
 * Aislamiento entre tests: antes de cada uno se vuelve a ejecutar BaseDatos.sql
 * dentro del contenedor. El script empieza con DROP TABLE IF EXISTS, asi que
 * cada test arranca con exactamente el estado que recibiria el evaluador,
 * contador de numeros de cuenta incluido. Sale mas caro que un DELETE FROM,
 * pero es lo unico que garantiza que no quede residuo de la prueba anterior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.show-sql=false",
        // Recortado respecto de los 3000 de produccion para que el test de
        // timeout no cueste tres segundos.
        "customers.service.timeout-ms=1000"
})
public abstract class AbstractIntegrationTest {

    /** Ruta que el contrato declara en su bloque servers. */
    private static final String BASE_PATH = "/api/v1";

    /**
     * Uno solo para toda la JVM: arrancar y parar un servidor HTTP por clase de
     * test es el tipo de coste que hace que nadie quiera correr la suite.
     * resetAll() en cada test lo deja limpio.
     */
    private static final WireMockServer CUSTOMER_SERVICE =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        CUSTOMER_SERVICE.start();
    }

    @DynamicPropertySource
    static void customerServiceUrl(DynamicPropertyRegistry registry) {
        // El puerto es dinamico, asi que la url no se puede escribir en un
        // properties: tiene que resolverse despues de arrancar WireMock.
        registry.add("customers.service.url", () -> CUSTOMER_SERVICE.baseUrl() + BASE_PATH);
    }

    @Autowired
    private MySQLContainer database;

    /**
     * El puerto real del servidor. Se lee de la propiedad y no con
     * @LocalServerPort porque esa anotacion cambio de paquete en Boot 4;
     * local.server.port la publica el propio contexto de test y es estable.
     */
    @Value("${local.server.port}")
    private int port;

    /** Cliente HTTP contra el servidor real, ya apuntando a /api/v1. */
    protected WebTestClient http;

    @BeforeEach
    void resetEnvironment() throws IOException, InterruptedException {
        CUSTOMER_SERVICE.resetAll();
        reloadDeliverableSchema();

        http = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port + BASE_PATH)
                // El default de 5s se queda corto la primera vez que el
                // contenedor calienta el pool de conexiones.
                .responseTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Reejecuta el entregable dentro del contenedor.
     *
     * Se lanza con el cliente mariadb y no por JDBC porque el script trae
     * CREATE DATABASE, USE y una vista: es el mismo camino que seguira el
     * evaluador, y ademas ningun partidor de sentencias tiene que adivinar
     * donde termina cada una.
     */
    private void reloadDeliverableSchema() throws IOException, InterruptedException {
        ExecResult result = database.execInContainer("sh", "-c",
                "mariadb -uroot -p" + database.getPassword()
                        + " < " + TestcontainersConfiguration.INIT_SCRIPT_PATH);

        if (result.getExitCode() != 0) {
            throw new IllegalStateException("BaseDatos.sql fallo al recargarse:\n"
                    + result.getStderr() + result.getStdout());
        }
    }

    // ------------------------------------------------------------- atajos HTTP

    /**
     * Alta de cuenta por el API, devolviendo el numero generado.
     *
     * Lo usan los tests que necesitan una cuenta como punto de partida pero cuyo
     * asunto no es el alta. Stubea el cliente por su cuenta: sin eso el create
     * responderia 404 y el fallo apuntaria al sitio equivocado.
     */
    protected String createAccount(String customerId, String accountType, String initialBalance) {
        givenCustomerExists(customerId);

        AccountDto created = http.post().uri("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountType":"%s","initialBalance":%s,"customerId":"%s"}
                        """.formatted(accountType, initialBalance, customerId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AccountDto.class)
                .returnResult()
                .getResponseBody();

        return created.getAccountNumber();
    }

    /** Alta de movimiento por el API, devolviendo el movimiento creado. */
    protected MovementDto createMovement(String accountNumber, String movementType, String value) {
        return http.post().uri("/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"accountNumber":"%s","movementType":"%s","value":%s}
                        """.formatted(accountNumber, movementType, value))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MovementDto.class)
                .returnResult()
                .getResponseBody();
    }

    /** Saldo disponible que reporta el API para una cuenta. */
    protected Double availableBalanceOf(String accountNumber) {
        return http.get().uri("/accounts/{accountNumber}", accountNumber)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AccountDto.class)
                .returnResult()
                .getResponseBody()
                .getAvailableBalance();
    }

    // ------------------------------------------------ dobles del otro servicio

    /** El cliente existe: 200 con la forma que espera CustomerResponse. */
    protected void givenCustomerExists(String customerId, String name, String identification) {
        CUSTOMER_SERVICE.stubFor(get(urlPathEqualTo(customerPath(customerId)))
                .willReturn(okJson("""
                        {"id":"%s","name":"%s","identification":"%s","status":true}
                        """.formatted(customerId, name, identification))));
    }

    /** Atajo para los tests a los que solo les importa que el cliente exista. */
    protected void givenCustomerExists(String customerId) {
        givenCustomerExists(customerId, "Cliente " + customerId, "0000000000");
    }

    /** 404 del otro servicio -> Optional vacio -> 404 nuestro. */
    protected void givenCustomerDoesNotExist(String customerId) {
        CUSTOMER_SERVICE.stubFor(get(urlPathEqualTo(customerPath(customerId)))
                .willReturn(aResponse().withStatus(404)));
    }

    /** 5xx del otro servicio -> CustomerServiceUnavailableException -> 502. */
    protected void givenCustomerServiceFails(String customerId) {
        CUSTOMER_SERVICE.stubFor(get(urlPathEqualTo(customerPath(customerId)))
                .willReturn(aResponse().withStatus(503)));
    }

    /** Respuesta mas lenta que customers.service.timeout-ms -> tambien 502. */
    protected void givenCustomerServiceTimesOut(String customerId) {
        CUSTOMER_SERVICE.stubFor(get(urlPathEqualTo(customerPath(customerId)))
                .willReturn(aResponse().withStatus(200).withFixedDelay(3_000)));
    }

    private static String customerPath(String customerId) {
        return BASE_PATH + "/customers/" + customerId;
    }
}
