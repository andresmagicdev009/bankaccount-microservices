package com.example.customerms;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Database used by the integration tests.
 *
 * The image is mariadb, not mysql: MariaDB is the real engine of the project in
 * every environment -XAMPP locally, mariadb:11.4 in docker-compose- and
 * account-service tests against the same one. Testing on MySQL 8 would validate
 * an engine that is never used.
 *
 * It is instantiated through MySQLContainer -not MariaDBContainer- on purpose:
 * that keeps the url as jdbc:mysql:// and the driver as mysql-connector-j,
 * exactly as in production.
 *
 * The version is pinned to the docker-compose one. No "latest": a moving image
 * turns a reproducible build into one that depends on the day.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	private static final DockerImageName IMAGE = DockerImageName
			.parse("mariadb:11.4")
			.asCompatibleSubstituteFor("mysql");

	@Bean
	@ServiceConnection
	public MySQLContainer mysqlContainer() {
		return new MySQLContainer(IMAGE);
	}

}
