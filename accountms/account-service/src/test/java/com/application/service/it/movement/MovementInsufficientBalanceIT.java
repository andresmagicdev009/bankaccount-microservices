package com.application.service.it.movement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.application.service.domain.account.entity.AccountType;
import com.application.service.domain.movement.entity.MovementType;
import com.application.service.infrastructure.persistence.jpa.entity.AccountEntity;
import com.application.service.infrastructure.persistence.jpa.entity.MovementEntity;
import com.application.service.infrastructure.persistence.jpa.repository.JpaAccountRepository;
import com.application.service.infrastructure.persistence.jpa.repository.JpaMovementRepository;
import com.application.service.it.AbstractIntegrationTest;
import com.application.service.interfaces.rest.dto.ErrorDto;
import com.application.service.interfaces.rest.dto.MovementCreateDto;
import com.application.service.interfaces.rest.dto.MovementDto;
import com.application.service.interfaces.rest.dto.MovementPatchDto;
import com.application.service.interfaces.rest.dto.MovementUpdateDto;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule F3 - "Saldo no disponible" end-to-end: HTTP -> MovementController ->
 * MovementService -> MovementHelpers.applyToBalance -> JPA adapter -> real
 * MariaDB (Testcontainers).
 *
 * Why as an INTEGRATION test and not only a unit one: F3 is not just the
 * "if resulting < 0" of MovementHelpers -MovementServiceTest already covers
 * that-. What can only be verified with the whole stack is the rest of the
 * rule:
 *
 *   1. that the domain exception reaches the client as a 422 carrying the
 *      literal text "Saldo no disponible" (GlobalExceptionHandler + contract);
 *   2. that the rejection leaves the database untouched, that is, that the
 *      transaction rolls the balance back and inserts no movement;
 *   3. that the rule covers the THREE doors moving the balance -create, edit
 *      and revert-, not only the POST;
 *   4. that two concurrent debits cannot slip past the rule, which is what
 *      justifies the SELECT ... FOR UPDATE of loadAccountForMovement.
 *
 * The real Netty server, the MariaDB container and the WebTestClient all come
 * from AbstractIntegrationTest, which documents every annotation.
 *
 * spring.webflux.base-path=/api/v1 already prefixes the routes and the
 * auto-configured WebTestClient carries it in its baseUrl: requests here target
 * /movements, not /api/v1/movements.
 *
 * WARNING: do NOT put @Transactional on this class. The JPA work runs on the
 * jdbc scheduler, on a different thread and a different connection, so the test
 * would neither see its own data nor roll back what the server writes. Cleanup
 * is explicit.
 */
class MovementInsufficientBalanceIT extends AbstractIntegrationTest {

    /** Relative path: the WebTestClient already carries /api/v1 in its baseUrl. */
    private static final String MOVEMENTS_URI = "/movements";

    /** Absolute path, the one the ErrorDto reports in its path field. */
    private static final String MOVEMENTS_PATH = "/api/v1/movements";

    /** Text required by the specification. Literal, untranslated and undecorated. */
    private static final String INSUFFICIENT_BALANCE = "Saldo no disponible";

    /** Fictional customer: the account table has no FK against customers_ms. */
    private static final String CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JpaAccountRepository accountRepository;

    @Autowired
    private JpaMovementRepository movementRepository;

    /**
     * schemas/BaseDatos.sql ships seed data (accounts 478758, 225487...), so
     * without cleaning, each test would inherit balances from the seed and from
     * the previous run. Movements first: movement has a FK to account.
     */
    @BeforeEach
    void cleanDatabase() {
        movementRepository.deleteAll();
        accountRepository.deleteAll();
    }

    // ------------------------------------------------------------------ SETUP

    /**
     * The account is inserted through the repository and not through
     * POST /accounts on purpose: creating an account through the API fires the
     * REST call to the customer microservice, which has nothing to do with F3
     * and would force setting up a double.
     */
    private AccountEntity givenAccountWithBalance(String accountNumber, String balance) {
        AccountEntity account = new AccountEntity();
        account.setAccountNumber(accountNumber);
        account.setAccountType(AccountType.SAVINGS);
        account.setInitialBalance(new BigDecimal(balance));
        account.setAvailableBalance(new BigDecimal(balance));
        account.setStatus(true);
        account.setCustomerId(CUSTOMER_ID);
        return accountRepository.save(account);
    }

    private MovementCreateDto movementPayload(String accountNumber,
            MovementCreateDto.MovementTypeEnum type, double value) {
        return new MovementCreateDto()
                .accountNumber(accountNumber)
                .movementType(type)
                .value(value);
    }

    /** A creation over HTTP expected to succeed: it leaves the account at a known balance. */
    private MovementDto createMovement(String accountNumber,
            MovementCreateDto.MovementTypeEnum type, double value) {
        return webTestClient.post()
                .uri(MOVEMENTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(movementPayload(accountNumber, type, value))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MovementDto.class)
                .returnResult()
                .getResponseBody();
    }

    // --------------------------------------------------------------- ASSERTS

    /**
     * The whole 422 contract in a single place: status, reason phrase, literal
     * message and path. The detail -available vs. requested balance- must NOT
     * appear: by design it goes to the log, never to the body.
     */
    private void assertInsufficientBalanceBody(ErrorDto error, String expectedPath) {
        assertThat(error).isNotNull();
        assertThat(error.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(error.getError()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase());
        assertThat(error.getMessage()).isEqualTo(INSUFFICIENT_BALANCE);
        assertThat(error.getPath()).isEqualTo(expectedPath);
        assertThat(error.getTimestamp()).isNotNull();
    }

    private void assertAvailableBalance(String accountNumber, String expected) {
        BigDecimal persisted = accountRepository.findById(accountNumber)
                .orElseThrow(() -> new AssertionError("La cuenta " + accountNumber + " no esta en la BD"))
                .getAvailableBalance();

        // isEqualByComparingTo and not isEqualTo: DECIMAL(15,2) comes back as
        // 1000.00 and new BigDecimal("1000") is not equals to it, only comparable.
        assertThat(persisted).isEqualByComparingTo(new BigDecimal(expected));
    }

    // ----------------------------------------------------------------- TESTS

    @Test
    @DisplayName("POST /movements: a debit greater than the balance returns 422 Saldo no disponible")
    void debitOverBalance_returns422AndLeavesDatabaseUntouched() {
        givenAccountWithBalance("900001", "1000.00");

        ErrorDto error = webTestClient.post()
                .uri(MOVEMENTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(movementPayload("900001", MovementCreateDto.MovementTypeEnum.DEBIT, 1500.00))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(ErrorDto.class)
                .returnResult()
                .getResponseBody();

        assertInsufficientBalanceBody(error, MOVEMENTS_PATH);

        // What really closes the rule: the rejection left no trace in the
        // database. If applyToBalance saved before validating, or the
        // transaction did not roll back, the balance would show as moved even
        // though the HTTP status says 422.
        assertAvailableBalance("900001", "1000.00");
        assertThat(movementRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /movements: a debit equal to the balance passes and leaves the account at zero")
    void debitEqualToBalance_isAccepted() {
        givenAccountWithBalance("900002", "1000.00");

        // Exact boundary of the rule: a NEGATIVE balance is rejected, not zero.
        // Without this case, a mistyped "resulting <= 0" would go unnoticed.
        MovementDto created = createMovement("900002", MovementCreateDto.MovementTypeEnum.DEBIT, 1000.00);

        assertThat(created).isNotNull();
        assertThat(created.getBalance()).isEqualTo(0.00);

        assertAvailableBalance("900002", "0.00");
        assertThat(movementRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /movements: one cent above the balance already returns 422")
    void debitOneCentOverBalance_returns422() {
        givenAccountWithBalance("900003", "1000.00");

        // The other half of the boundary. The 0.01 also proves the comparison
        // happens in BigDecimal: in double, 1000.00 - 1000.01 gives
        // -0.009999999999990905 and any tolerance would let it through.
        webTestClient.post()
                .uri(MOVEMENTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(movementPayload("900003", MovementCreateDto.MovementTypeEnum.DEBIT, 1000.01))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody(ErrorDto.class)
                .value(error -> assertInsufficientBalanceBody(error, MOVEMENTS_PATH));

        assertAvailableBalance("900003", "1000.00");
        assertThat(movementRepository.count()).isZero();
    }

    @Test
    @DisplayName("The rule is evaluated on the running balance, not on the initial one")
    void debitOverRunningBalance_returns422() {
        givenAccountWithBalance("900004", "1000.00");

        createMovement("900004", MovementCreateDto.MovementTypeEnum.DEBIT, 600.00);   // -> 400.00
        createMovement("900004", MovementCreateDto.MovementTypeEnum.CREDIT, 100.00);  // -> 500.00

        // 700 fits in the initial balance (1000) but not in the available one
        // (500): if the validation looked at initial_balance instead of
        // available_balance, this request would pass and the account would end
        // up overdrawn.
        webTestClient.post()
                .uri(MOVEMENTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(movementPayload("900004", MovementCreateDto.MovementTypeEnum.DEBIT, 700.00))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody(ErrorDto.class)
                .value(error -> assertInsufficientBalanceBody(error, MOVEMENTS_PATH));

        assertAvailableBalance("900004", "500.00");
        // Both valid movements are still there: the 422 did not touch the history.
        assertThat(movementRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("PUT /movements/{id}: raising the debit above the balance returns 422")
    void updateDebitOverBalance_returns422AndKeepsMovement() {
        givenAccountWithBalance("900005", "1000.00");
        MovementDto movement = createMovement("900005", MovementCreateDto.MovementTypeEnum.DEBIT, 100.00);
        String movementId = movement.getMovementId().toString();

        // Second door to the balance: the edit applies the DELTA (new - old).
        // Here it is -5000 - (-100) = -4900 over 900.00 -> negative -> F3.
        webTestClient.put()
                .uri(MOVEMENTS_URI + "/{movementId}", movementId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MovementUpdateDto()
                        .movementType(MovementUpdateDto.MovementTypeEnum.DEBIT)
                        .value(5000.00))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody(ErrorDto.class)
                .value(error -> assertInsufficientBalanceBody(error, MOVEMENTS_PATH + "/" + movementId));

        // The original movement stays exactly as it was: neither its value nor
        // its historical balance moved with the rejected update.
        MovementEntity persisted = movementRepository.findById(movementId)
                .orElseThrow(() -> new AssertionError("El movimiento desaparecio de la base de datos"));

        assertThat(persisted.getMovementType()).isEqualTo(MovementType.DEBIT);
        assertThat(persisted.getValue()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(persisted.getBalance()).isEqualByComparingTo(new BigDecimal("900.00"));

        assertAvailableBalance("900005", "900.00");
    }

    @Test
    @DisplayName("PATCH /movements/{id}: raising only the value above the balance returns 422")
    void patchValueOverBalance_returns422() {
        givenAccountWithBalance("900006", "500.00");
        MovementDto movement = createMovement("900006", MovementCreateDto.MovementTypeEnum.DEBIT, 50.00);
        String movementId = movement.getMovementId().toString();

        // The PATCH sends only value and keeps the type: it checks that the
        // partial branch goes through the same balance door as the PUT.
        webTestClient.patch()
                .uri(MOVEMENTS_URI + "/{movementId}", movementId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new MovementPatchDto().value(900.00))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody(ErrorDto.class)
                .value(error -> assertInsufficientBalanceBody(error, MOVEMENTS_PATH + "/" + movementId));

        assertThat(movementRepository.findById(movementId).orElseThrow().getValue())
                .isEqualByComparingTo(new BigDecimal("50.00"));
        assertAvailableBalance("900006", "450.00");
    }

    @Test
    @DisplayName("DELETE /movements/{id}: a reversal that would leave a negative balance returns 422")
    void deleteCreditThatWouldOverdraw_returns422() {
        givenAccountWithBalance("900007", "100.00");

        // Third door to the balance: deleting applies the opposite sign, so
        // reverting a CREDIT SUBTRACTS and has to honour F3 as well.
        //
        // The row is seeded by hand because this state cannot be reached through
        // the API: it needs the available balance (100) to be lower than the
        // credit left as the last movement (500). It is the shape of the seed
        // data of the deliverable, where available_balance is set by hand.
        MovementEntity credit = new MovementEntity();
        credit.setMovementId(UUID.randomUUID().toString());
        credit.setDate(LocalDateTime.now());
        credit.setMovementType(MovementType.CREDIT);
        credit.setValue(new BigDecimal("500.00"));
        credit.setBalance(new BigDecimal("600.00"));
        credit.setAccountNumber("900007");
        movementRepository.save(credit);

        webTestClient.delete()
                .uri(MOVEMENTS_URI + "/{movementId}", credit.getMovementId())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody(ErrorDto.class)
                .value(error -> assertInsufficientBalanceBody(error,
                        MOVEMENTS_PATH + "/" + credit.getMovementId()));

        // The 422 aborts the whole deletion: the row is still there and the balance did not move.
        assertThat(movementRepository.existsById(credit.getMovementId())).isTrue();
        assertAvailableBalance("900007", "100.00");
    }

    @Test
    @DisplayName("Two concurrent debits: one passes, the other 422, and the account never goes negative")
    void concurrentDebits_onlyOneSucceeds() throws Exception {
        givenAccountWithBalance("900008", "1000.00");

        // Each debit fits on its own (700 <= 1000) but the two together do not.
        // Without the SELECT ... FOR UPDATE of loadAccountForMovement, both
        // transactions would read 1000, both would pass F3 and the second COMMIT
        // would leave the account at -400: the classic lost update.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLine = new CountDownLatch(1);

        Callable<Integer> debit = () -> {
            startLine.await();   // los dos hilos salen a la vez
            return webTestClient.post()
                    .uri(MOVEMENTS_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(movementPayload("900008", MovementCreateDto.MovementTypeEnum.DEBIT, 700.00))
                    .exchange()
                    .returnResult(MovementDto.class)
                    .getStatus()
                    .value();
        };

        try {
            Future<Integer> first = pool.submit(debit);
            Future<Integer> second = pool.submit(debit);
            startLine.countDown();

            List<Integer> statuses = List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS));

            assertThat(statuses).containsExactlyInAnyOrder(
                    HttpStatus.CREATED.value(),
                    HttpStatus.UNPROCESSABLE_CONTENT.value());
        } finally {
            pool.shutdownNow();
        }

        assertAvailableBalance("900008", "300.00");
        assertThat(movementRepository.count()).isEqualTo(1);
    }
}
