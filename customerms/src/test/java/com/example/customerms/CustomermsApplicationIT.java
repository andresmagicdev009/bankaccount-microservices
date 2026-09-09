package com.example.customerms;

import org.junit.jupiter.api.Test;

/**
 * Smoke test. The method is empty on purpose: what is being tested is that the
 * context starts, and that happens before entering the body.
 *
 * It catches broken startups -a bean that cannot be built, a misspelled
 * property, a Flyway migration that does not apply, an entity out of sync with
 * the schema- with a single readable failure instead of six functional tests
 * falling at once.
 *
 * It extends AbstractIntegrationTest to share the context (and the container)
 * with the rest of the integration tests.
 */
class CustomermsApplicationIT extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
