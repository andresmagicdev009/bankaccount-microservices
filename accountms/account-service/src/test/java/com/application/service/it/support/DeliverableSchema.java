package com.application.service.it.support;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Localiza schemas/BaseDatos.sql, el script que se entrega en la prueba tecnica.
 *
 * Las pruebas de integracion corren contra ESE archivo y no contra las
 * migraciones Flyway: si el entregable y las entidades se separan, la suite
 * tiene que enterarse.
 *
 * La ruta se busca subiendo desde el directorio de trabajo -que es el modulo si
 * lanzas Maven aqui, y la raiz del repo si lo lanzas desde arriba- en vez de
 * escribir "../../schemas": asi funciona en los dos casos.
 */
public final class DeliverableSchema {

    /** Ruta del entregable relativa a la raiz del repositorio. */
    private static final String RELATIVE_PATH = "schemas/BaseDatos.sql";

    private DeliverableSchema() {
    }

    public static Path path() {
        Path directory = Path.of("").toAbsolutePath();

        while (directory != null) {
            Path candidate = directory.resolve(RELATIVE_PATH);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }

        throw new IllegalStateException(
                "No se encontro " + RELATIVE_PATH + " subiendo desde " + Path.of("").toAbsolutePath());
    }
}
