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
 * End-to-end integration test for Customer: HTTP -> controller -> service ->
 * JPA adapter -> real MySQL (Testcontainers).
 *
 * The real Netty server, the MySQL container and the WebTestClient all come
 * from AbstractIntegrationTest, which documents every annotation. Only the test
 * cases live here.
 *
 * spring.webflux.base-path=/api/v1 (application.properties) already prefixes
 * the routes, and the auto-configured WebTestClient carries it in its baseUrl:
 * requests here target /customers, not /api/v1/customers.
 *
 * WARNING: do NOT put @Transactional on this class. The JPA work runs on the
 * jdbc scheduler, on a different thread and a different connection, so the test
 * would neither see its own data nor roll back what the server writes. Cleanup
 * is explicit.
 */
class CustomerIT extends AbstractIntegrationTest {

    /**
     * Relative path. The auto-configured WebTestClient already carries the
     * base-path (/api/v1) in its baseUrl, so repeating it here would produce
     * /api/v1/api/v1/...
     */
    private static final String CUSTOMERS_URI = "/customers";

    /** Location is built by the controller by hand, with the full prefix. */
    private static final String CUSTOMERS_PATH = "/api/v1/customers";

    /** HTTP client: enters through the same port a real consumer would. */
    @Autowired
    private WebTestClient webTestClient;

    /** Persistence side: queried directly to prove the data REALLY landed in the DB. */
    @Autowired
    private JpaCustomerRepository repository;

    /**
     * Raw SQL to inspect the person table, which has no repository of its own:
     * the only way to check the cascading delete of the JOINED inheritance.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // identification is UNIQUE: without cleanup the second test hits a 409.
        // Deleting CustomerEntity also deletes its person row (JOINED inheritance).
        repository.deleteAll();
    }

    /** Builds the customer payload used across the integration tests. */
    private CustomerCreateDto newCustomerPayload() {
        return new CustomerCreateDto()
                .name("Jose Lema")
                .identification("0102030405")
                .password("1234")
                .gender(GenderDto.MALE)
                .address("Otavalo sn y principal")
                .phone("098254785");
    }

    /** Creation over HTTP, reused by the tests that need an existing customer. */
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

    // First test: validates customer creation and that it is persisted in the database.

    @Test
    @DisplayName("POST /customers persists the customer and returns 201 + Location")
    void createCustomer_persistsAndReturns201() {
        CustomerCreateDto payload = newCustomerPayload();

        // --- ACT: real HTTP request against the test's Netty server ---
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

        // --- ASSERT HTTP layer: the body honours the contract ---
        assertThat(body).isNotNull();
        assertThat(body.getId()).isNotNull();               // server-side generated UUID
        assertThat(body.getName()).isEqualTo(payload.getName());
        assertThat(body.getIdentification()).isEqualTo(payload.getIdentification());
        assertThat(body.getGender()).isEqualTo(payload.getGender());
        assertThat(body.getAddress()).isEqualTo(payload.getAddress());
        assertThat(body.getPhone()).isEqualTo(payload.getPhone());
        assertThat(body.getStatus()).isTrue();              // the service defaults it to true when null
        assertThat(body.getCreatedAt()).isNotNull();        // @CreationTimestamp on PersonEntity
        assertThat(body.getUpdatedAt()).isNotNull();

        // Location is built by the controller as /api/v1/customers/{id}: checked
        // after reading the body because the id is unknown before the request.
        assertThat(result.getResponseHeaders().getLocation())
                .isEqualTo(URI.create(CUSTOMERS_PATH + "/" + body.getId()));

        // --- ASSERT persistence layer: this is what closes the "all the way to the DB" claim ---
        CustomerEntity persisted = repository.findById(body.getId().toString())
                .orElseThrow(() -> new AssertionError("The customer did not land in the database"));

        assertThat(persisted.getName()).isEqualTo(payload.getName());
        assertThat(persisted.getIdentification()).isEqualTo(payload.getIdentification());
        assertThat(persisted.getGender()).isEqualTo(Gender.MALE);
        assertThat(persisted.getAddress()).isEqualTo(payload.getAddress());
        assertThat(persisted.getPhone()).isEqualTo(payload.getPhone());
        assertThat(persisted.getStatus()).isTrue();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        // password is writeOnly in the contract: it never comes back in the DTO,
        // but it is stored. Today it is persisted in clear text; if hashing is
        // introduced later, this assert changes to checking it is NOT the plain text.
        assertThat(persisted.getPassword()).isEqualTo(payload.getPassword());

        // A single row: the POST did not duplicate anything.
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /customers/{id} reads back from the DB the customer created by POST")
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

        // Full round trip: DB -> CustomerPersistenceMapper -> domain ->
        // CustomerMapper -> JSON. If any layer drops a field, it shows up here.
        // Timestamps are checked apart: the POST response carries them from the
        // in-memory instance, with nanos, while the MySQL DATETIME column stores
        // them truncated.
        assertThat(fetched).usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(created);

        assertThat(fetched.getCreatedAt()).isCloseTo(created.getCreatedAt(), within(1, ChronoUnit.SECONDS));
        assertThat(fetched.getUpdatedAt()).isCloseTo(created.getUpdatedAt(), within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("POST with a duplicate identification returns 409 and inserts no second row")
    void createCustomer_duplicateIdentification_returns409() {
        CustomerCreateDto payload = newCustomerPayload();
        createViaApi(payload);

        // Same identification: CustomerService.create detects it with
        // existsByIdentification and throws DuplicateIdentificationException, which
        // GlobalHandlerException translates into a 409 through its CONFLICT ErrorType.
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

        // What matters about the 409: the DB was not left with two rows.
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("PUT /customers/{id} updates the row in the DB")
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
        assertThat(updated.getId()).isEqualTo(created.getId());   // the PUT does not replace the id
        assertThat(updated.getName()).isEqualTo(update.getName());
        assertThat(updated.getGender()).isEqualTo(update.getGender());
        assertThat(updated.getAddress()).isEqualTo(update.getAddress());
        assertThat(updated.getPhone()).isEqualTo(update.getPhone());
        assertThat(updated.getStatus()).isFalse();

        // The row really changed, not just the response.
        CustomerEntity persisted = repository.findById(created.getId().toString())
                .orElseThrow(() -> new AssertionError("The customer vanished from the database"));

        assertThat(persisted.getName()).isEqualTo(update.getName());
        assertThat(persisted.getGender()).isEqualTo(Gender.OTHER);
        assertThat(persisted.getAddress()).isEqualTo(update.getAddress());
        assertThat(persisted.getPhone()).isEqualTo(update.getPhone());
        assertThat(persisted.getStatus()).isFalse();
        // password is writeOnly: it never comes back in the DTO, so the only way
        // to check the PUT replaced it is to look at the row.
        assertThat(persisted.getPassword()).isEqualTo("nuevo-password");
        // updated_at is refreshed by @UpdateTimestamp; created_at is updatable=false.
        // Non-strict comparison: a DATETIME without precision can leave both in the
        // same second when the test runs fast.
        assertThat(persisted.getUpdatedAt()).isAfterOrEqualTo(persisted.getCreatedAt());

        // UPDATE, not INSERT.
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE /customers/{id} removes both the customer and person rows")
    void deleteCustomer_removesRow() {
        CustomerDto created = createViaApi(newCustomerPayload());
        String id = created.getId().toString();

        webTestClient.delete()
                .uri(CUSTOMERS_URI + "/{id}", created.getId())
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        assertThat(repository.existsById(id)).isFalse();

        // The person row goes away too (FK ON DELETE CASCADE from V1__init_schema.sql).
        // Without this assert, a delete leaving the parent row orphaned would slip through.
        Long personRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM person WHERE id = ?", Long.class, id);
        assertThat(personRows).isZero();
    }

    @Test
    @DisplayName("GET /customers/{id} for an unknown id returns 404")
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
                    // The path carries the request's real prefix, not the relative route.
                    assertThat(error.getPath()).isEqualTo(CUSTOMERS_PATH + "/" + unknownId);
                    assertThat(error.getTimestamp()).isNotNull();
                });

        assertThat(repository.count()).isZero();
    }
}
