package com.application.service.it.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Locates schemas/BaseDatos.sql, the deliverable script of the technical test.
 *
 * The schema of the integration tests comes from that file and not from the
 * Flyway migrations: what gets validated this way is the deliverable the
 * reviewer is going to run. If the script and the entities drift apart,
 * ddl-auto=validate breaks the context startup and the failure shows up in the
 * first IT.
 *
 * The path is found by walking up the parents of the working directory instead
 * of writing "../../schemas/BaseDatos.sql": the working directory changes with
 * whoever launches the suite -Maven inside the module, the IDE at the root of
 * the repo- and a fixed relative path only works in one of the two cases.
 */
public final class DeliverableSchema {

    /** Path of the script, relative to the root of the repository. */
    private static final String RELATIVE_PATH = "schemas/BaseDatos.sql";

    private DeliverableSchema() {
    }

    /**
     * @return absolute path of the script.
     * @throws IllegalStateException if it shows up in no ancestor: without a
     *         schema the container starts empty and the real error would be
     *         buried in a Hibernate validation failure.
     */
    public static Path path() {
        Path current = Paths.get("").toAbsolutePath();

        while (current != null) {
            Path candidate = current.resolve(RELATIVE_PATH);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }

        throw new IllegalStateException(
                "Could not find " + RELATIVE_PATH + " walking up from " + Paths.get("").toAbsolutePath());
    }
}
