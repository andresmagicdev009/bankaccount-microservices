package com.application.service.it;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;

import com.application.service.TestcontainersConfiguration;

/**
 * Base class of the integration tests: the whole application against a real
 * MariaDB.
 *
 * It exists because of Spring's context cache: the cache key is built from the
 * combined configuration of the test class, so two classes with identical
 * annotations share a context -and therefore a single container, started once
 * for the entire suite. As soon as one class changes a property or the
 * webEnvironment, a new context is opened and another Docker startup is paid
 * for. Extending this class keeps that from happening by accident.
 *
 * What each annotation contributes:
 *
 * - @Import(TestcontainersConfiguration): starts the mariadb:11.4 container and
 *   @ServiceConnection points spring.datasource.* at it. The schema is created
 *   by schemas/BaseDatos.sql -the deliverable- and ddl-auto=validate checks
 *   that the entities match that script.
 * - RANDOM_PORT: a real Netty server, not a mock. The request crosses the event
 *   loop and BlockingBridge exactly as in production, which is precisely where
 *   the translation of a domain exception into its HTTP status could be lost.
 * - @AutoConfigureWebTestClient: in Boot 4 the WebTestClient support moved to
 *   its own module and RANDOM_PORT alone no longer provides the bean.
 *
 * spring.flyway.enabled=false: the deliverable script already created the
 * tables; with Flyway switched on, V1 would try to create them again and the
 * context would not start.
 *
 * Subclasses must NOT be annotated with @SpringBootTest nor repeat these
 * annotations: any difference breaks the shared context.
 */
@Import(TestcontainersConfiguration.class)
@AutoConfigureWebTestClient
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=false",
                "spring.test.webtestclient.timeout=30s"
        })
public abstract class AbstractIntegrationTest {
}
