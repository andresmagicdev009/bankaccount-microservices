package com.application.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import com.application.service.it.support.DeliverableSchema;

/**
 * Base de datos de las pruebas de integracion.
 *
 * Dos decisiones que no son cosmeticas:
 *
 * 1. La imagen es mariadb, no mysql. El motor real del proyecto es MariaDB en
 *    los dos entornos -XAMPP en local, mariadb:11.4 en docker-compose- y el
 *    dialecto configurado es MariaDBDialect por el "FOR UPDATE" del bloqueo
 *    pesimista. Probar sobre MySQL 8 validaria un motor que no se usa.
 *    Se instancia con MySQLContainer -no con MariaDBContainer- a proposito:
 *    asi la url sigue siendo jdbc:mysql:// y el driver sigue siendo
 *    mysql-connector-j, exactamente como en produccion.
 *
 * 2. El esquema lo crea schemas/BaseDatos.sql, el entregable de la prueba
 *    tecnica, montado en el directorio de arranque del contenedor. Flyway va
 *    apagado en la suite (ver AbstractIntegrationTest): si estuviera encendido
 *    intentaria aplicar V1 sobre tablas que ya existen.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** El entrypoint de la imagen ejecuta como root todo lo que haya aqui. */
    public static final String INIT_SCRIPT_PATH = "/docker-entrypoint-initdb.d/01-BaseDatos.sql";

    /**
     * La version se fija a la del docker-compose. Nada de "latest": una imagen
     * movil convierte un build reproducible en uno que depende del dia.
     */
    private static final DockerImageName IMAGE = DockerImageName
            .parse("mariadb:11.4")
            .asCompatibleSubstituteFor("mysql");

    /**
     * El nombre de la base tiene que ser el mismo que usa el script -accounts_ms-
     * o el entrypoint crearia "test" y el GRANT del usuario de pruebas no
     * alcanzaria a las tablas del entregable.
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
