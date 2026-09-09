package com.application.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import com.application.service.it.support.DeliverableSchema;

/**
 * Database used by the integration tests.
 *
 * Two decisions that are not cosmetic:
 *
 * 1. The image is mariadb, not mysql. The real engine of the project is MariaDB
 *    in both environments -XAMPP locally, mariadb:11.4 in docker-compose- and
 *    the configured dialect is MariaDBDialect because of the "FOR UPDATE" of
 *    the pessimistic lock. Testing on MySQL 8 would validate an engine that is
 *    never used. It is instantiated through MySQLContainer -not
 *    MariaDBContainer- on purpose: that keeps the url as jdbc:mysql:// and the
 *    driver as mysql-connector-j, exactly as in production.
 *
 * 2. The schema is created by schemas/BaseDatos.sql, the deliverable of the
 *    technical test, mounted in the startup directory of the container. Flyway
 *    is switched off in the suite (see AbstractIntegrationTest): switched on it
 *    would try to apply V1 over tables that already exist.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** The entrypoint of the image runs everything placed here as root. */
    public static final String INIT_SCRIPT_PATH = "/docker-entrypoint-initdb.d/01-BaseDatos.sql";

    /**
     * The version is pinned to the docker-compose one. No "latest": a moving
     * image turns a reproducible build into one that depends on the day.
     */
    private static final DockerImageName IMAGE = DockerImageName
            .parse("mariadb:11.4")
            .asCompatibleSubstituteFor("mysql");

    /**
     * The database name has to be the one the script uses -accounts_ms- or the
     * entrypoint would create "test" and the GRANT of the test user would not
     * reach the tables of the deliverable.
     */
    @Bean
    @ServiceConnection
    MySQLContainer databaseContainer() {
        return new MySQLContainer(IMAGE)
                .withDatabaseName("accounts_ms")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(DeliverableSchema.path()),
                        INIT_SCRIPT_PATH);
    }
}
