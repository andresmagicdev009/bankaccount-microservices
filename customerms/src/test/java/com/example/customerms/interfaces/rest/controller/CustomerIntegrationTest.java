package com.example.customerms.interfaces.rest.controller;

import com.example.customerms.AbstractIntegrationTest;
import com.example.customerms.domain.person.entity.Gender;
import com.example.customerms.infrastructure.persistence.jpa.entity.CustomerEntity;
import com.example.customerms.infrastructure.persistence.jpa.repository.JpaCustomerRepository;
import com.example.customerms.interfaces.rest.dto.CustomerCreateDto;
import com.example.customerms.interfaces.rest.dto.CustomerDto;
import com.example.customerms.interfaces.rest.dto.CustomerUpdateDto;
import com.example.customerms.interfaces.rest.dto.ErrorDto;
import com.example.customerms.interfaces.rest.dto.GenderDto;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Prueba de integracion end-to-end de Cliente: HTTP -> controller -> service ->
 * adapter JPA -> MySQL real (Testcontainers).
 *
 * El servidor Netty real, el contenedor MySQL y el WebTestClient vienen de
 * AbstractIntegrationTest, que documenta cada anotacion. Aqui solo van los
 * casos de prueba.
 *
 * spring.webflux.base-path=/api/v1 (application.properties) ya prefija las
 * rutas, y el WebTestClient autoconfigurado lo lleva en su baseUrl: aqui se
 * pide /customers, no /api/v1/customers.
 *
 * OJO: NO poner @Transactional en esta clase. El trabajo JPA corre en el
 * scheduler jdbc, en otro hilo y otra conexion, asi que no se veria la data del
 * test ni se revertiria lo que escriba el servidor. La limpieza es explicita.
 */
class CustomerIntegrationTest extends AbstractIntegrationTest {

    /**
     * Ruta relativa. El WebTestClient autoconfigurado ya trae el base-path
     * (/api/v1) en su baseUrl, asi que repetirlo aqui produce /api/v1/api/v1/...
     */
    private static final String CUSTOMERS_URI = "/customers";

    /** Location la arma el controller con el prefijo completo, a mano. */
    private static final String CUSTOMERS_PATH = "/api/v1/customers";

    /** Cliente HTTP: entra por el mismo puerto que un consumidor real. */
    @Autowired
    private WebTestClient webTestClient;

    /** Lado persistencia: se consulta directo para probar que el dato QUEDO en la BD. */
    @Autowired
    private JpaCustomerRepository repository;

    /**
     * SQL crudo para mirar la tabla person, que no tiene repositorio propio: es la
     * unica forma de comprobar el borrado en cascada de la herencia JOINED.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // identification es UNIQUE: sin limpiar, el segundo test choca con 409.
        // Borrar CustomerEntity borra tambien su fila en person (herencia JOINED).
        repository.deleteAll();
    }

    // Funcion que permite la creacion de un usuario para la prueba de integracion
    private CustomerCreateDto newCustomerPayload() {
        return new CustomerCreateDto()
                .name("Jose Lema")
                .identification("0102030405")
                .password("1234")
                .gender(GenderDto.MALE)
                .address("Otavalo sn y principal")
                .phone("098254785");
    }

    /** Alta por HTTP, reutilizada por los tests que necesitan un cliente ya existente. */
    private CustomerDto createViaApi(CustomerCreateDto payload) {
        return webTestClient.post()
                .uri(CUSTOMERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerDto.class)
                .returnResult()
                .getResponseBody();
    }


    // -------------------- TESTS --------------------

    // Primer test para validar la creacion de un cliente y que se persista en la base de datos
     
    @Test
    @DisplayName("POST /customers persiste el cliente y lo devuelve con 201 + Location")
    void createCustomer_persistsAndReturns201() {
        CustomerCreateDto payload = newCustomerPayload();

        // --- ACT: request HTTP real contra el servidor Netty del test ---
        EntityExchangeResult<CustomerDto> result = webTestClient.post()
                .uri(CUSTOMERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(CustomerDto.class)
                .returnResult();

        CustomerDto body = result.getResponseBody();

        // --- ASSERT capa HTTP: el cuerpo respeta el contrato ---
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();               // UUID generado server-side
        assertThat(body.getName()).isEqualTo(payload.getName());
        assertThat(body.getIdentification()).isEqualTo(payload.getIdentification());
        assertThat(body.getGender()).isEqualTo(payload.getGender());
        assertThat(body.getAddress()).isEqualTo(payload.getAddress());
        assertThat(body.getPhone()).isEqualTo(payload.getPhone());
        assertThat(body.getStatus()).isTrue();              // el service pone true cuando llega null
        assertThat(body.getCreatedAt()).isNotNull();        // @CreationTimestamp de PersonEntity
        assertThat(body.getUpdatedAt()).isNotNull();

        // Location lo arma el controller como /api/v1/customers/{id}: se comprueba
        // despues de leer el cuerpo porque el id no se conoce antes del request.
        assertThat(result.getResponseHeaders().getLocation())
                .isEqualTo(URI.create(CUSTOMERS_PATH + "/" + body.getId()));

        // --- ASSERT capa persistencia: esto es lo que cierra el "hasta la BD" ---
        CustomerEntity persisted = repository.findById(body.getId().toString())
                .orElseThrow(() -> new AssertionError("El cliente no quedo en la base de datos"));

        assertThat(persisted.getName()).isEqualTo(payload.getName());
        assertThat(persisted.getIdentification()).isEqualTo(payload.getIdentification());
        assertThat(persisted.getGender()).isEqualTo(Gender.MALE);
        assertThat(persisted.getAddress()).isEqualTo(payload.getAddress());
        assertThat(persisted.getPhone()).isEqualTo(payload.getPhone());
        assertThat(persisted.getStatus()).isTrue();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        // password es writeOnly en el contrato: no vuelve en el DTO, pero si se guarda.
        // Hoy se persiste en claro; si mas adelante entra hashing, este assert cambia
        // a comprobar que NO es igual al texto plano.
        assertThat(persisted.getPassword()).isEqualTo(payload.getPassword());

        // Una sola fila: el POST no duplico nada.
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /customers/{id} lee de la BD el cliente creado por POST")
    void getCustomer_readsBackWhatWasPersisted() {
        CustomerDto created = createViaApi(newCustomerPayload());

        CustomerDto fetched = webTestClient.get()
                .uri(CUSTOMERS_URI + "/{id}", created.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(fetched).isNotNull();

        // Viaje de vuelta completo: BD -> CustomerPersistenceMapper -> dominio ->
        // CustomerMapper -> JSON. Si alguna capa pierde un campo, salta aqui.
        // Los timestamps van aparte: la respuesta del POST los trae de la instancia
        // en memoria, con nanos, y la columna DATETIME de MySQL los guarda truncados.
        assertThat(fetched).usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(created);

        assertThat(fetched.getCreatedAt()).isCloseTo(created.getCreatedAt(), within(1, ChronoUnit.SECONDS));
        assertThat(fetched.getUpdatedAt()).isCloseTo(created.getUpdatedAt(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("POST con identification duplicada devuelve 409 y no inserta segunda fila")
    void createCustomer_duplicateIdentification_returns409() {
        CustomerCreateDto payload = newCustomerPayload();
        createViaApi(payload);

        // Misma identification: CustomerService.create lo detecta con
        // existsByIdentification y lanza DuplicateIdentificationException, que
        // GlobalHandlerException traduce a 409 por su ErrorType CONFLICT.
        webTestClient.post()
                .uri(CUSTOMERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newCustomerPayload())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorDto.class)
                .value(error -> {
                    assertThat(error.getStatus()).isEqualTo(409);
                    assertThat(error.getError()).isEqualTo("Conflict");
                    assertThat(error.getMessage()).contains(payload.getIdentification());
                    assertThat(error.getPath()).isEqualTo(CUSTOMERS_PATH);
                    assertThat(error.getTimestamp()).isNotNull();
                });

        // Lo que importa del 409: la BD no quedo con dos filas.
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("PUT /customers/{id} actualiza la fila en BD")
    void updateCustomer_persistsChanges() {
        CustomerDto created = createViaApi(newCustomerPayload());

        CustomerUpdateDto update = new CustomerUpdateDto()
                .name("Jose Lema Editado")
                .identification(created.getIdentification())
                .password("nuevo-password")
                .gender(GenderDto.OTHER)
                .address("Amazonas y NNUU")
                .phone("097777777")
                .status(false);

        CustomerDto updated = webTestClient.put()
                .uri(CUSTOMERS_URI + "/{id}", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(update)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(created.getId());   // el PUT no reemplaza el id
        assertThat(updated.getName()).isEqualTo(update.getName());
        assertThat(updated.getGender()).isEqualTo(update.getGender());
        assertThat(updated.getAddress()).isEqualTo(update.getAddress());
        assertThat(updated.getPhone()).isEqualTo(update.getPhone());
        assertThat(updated.getStatus()).isFalse();

        // La fila se modifico de verdad, no solo la respuesta.
        CustomerEntity persisted = repository.findById(created.getId().toString())
                .orElseThrow(() -> new AssertionError("El cliente desaparecio de la base de datos"));

        assertThat(persisted.getName()).isEqualTo(update.getName());
        assertThat(persisted.getGender()).isEqualTo(Gender.OTHER);
        assertThat(persisted.getAddress()).isEqualTo(update.getAddress());
        assertThat(persisted.getPhone()).isEqualTo(update.getPhone());
        assertThat(persisted.getStatus()).isFalse();
        // password es writeOnly: no vuelve en el DTO, asi que la unica forma de
        // comprobar que el PUT lo reemplazo es mirar la fila.
        assertThat(persisted.getPassword()).isEqualTo("nuevo-password");
        // updated_at lo refresca @UpdateTimestamp; created_at es updatable=false.
        // Comparacion no estricta: DATETIME sin precision puede dejar ambos en el
        // mismo segundo cuando el test corre rapido.
        assertThat(persisted.getUpdatedAt()).isAfterOrEqualTo(persisted.getCreatedAt());

        // UPDATE, no INSERT.
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE /customers/{id} borra las filas customer y person")
    void deleteCustomer_removesRow() {
        CustomerDto created = createViaApi(newCustomerPayload());
        String id = created.getId().toString();

        webTestClient.delete()
                .uri(CUSTOMERS_URI + "/{id}", created.getId())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        assertThat(repository.existsById(id)).isFalse();

        // La fila de person tambien se va (FK ON DELETE CASCADE de V1__init_schema.sql).
        // Sin este assert, un borrado que dejara huerfana la fila padre pasaria inadvertido.
        Long personRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM person WHERE id = ?", Long.class, id);
        assertThat(personRows).isZero();
    }

    @Test
    @DisplayName("GET /customers/{id} inexistente devuelve 404")
    void getCustomer_notFound_returns404() {
        UUID unknownId = UUID.randomUUID();

        webTestClient.get()
                .uri(CUSTOMERS_URI + "/{id}", unknownId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorDto.class)
                .value(error -> {
                    assertThat(error.getStatus()).isEqualTo(404);
                    assertThat(error.getError()).isEqualTo("Not Found");
                    assertThat(error.getMessage()).contains(unknownId.toString());
                    // El path lleva el prefijo real de la peticion, no la ruta relativa.
                    assertThat(error.getPath()).isEqualTo(CUSTOMERS_PATH + "/" + unknownId);
                    assertThat(error.getTimestamp()).isNotNull();
                });

        assertThat(repository.count()).isZero();
    }
}
