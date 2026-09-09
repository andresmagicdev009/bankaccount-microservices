package com.example.customerms;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;

/**
 * Base class for the tests that need the whole application against a real MySQL.
 *
 * It exists because of Spring's context cache: the cache key is built from the
 * combined configuration of the test class, so two classes with identical
 * annotations share a context -and therefore a single MySQL container, started
 * once for the entire suite. As soon as one class changes a property or the
 * webEnvironment, a new context is opened and another Docker startup is paid
 * for. Extending this class keeps that from happening by accident.
 *
 * What each annotation contributes:
 *
 * - @Import(TestcontainersConfiguration): starts the container, and
 *   @ServiceConnection points spring.datasource.* at it. Flyway applies
 *   V1__init_schema.sql and ddl-auto=validate checks that the entities match
 *   that migration.
 * - RANDOM_PORT: a real Netty server, not a mock. The request crosses the event
 *   loop and BlockingBridge exactly as it does in production.
 * - @AutoConfigureWebTestClient: in Boot 4 the WebTestClient support moved to
 *   its own module and RANDOM_PORT alone no longer provides the bean.
 *
 * Subclasses must NOT be annotated with @SpringBootTest nor repeat these
 * annotations: any difference breaks the shared context.
 */
@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.test.webtestclient.timeout=30s")
public abstract class AbstractIntegrationTest {
}
