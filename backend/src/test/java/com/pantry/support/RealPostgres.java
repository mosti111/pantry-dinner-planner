package com.pantry.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Provides a real PostgreSQL server for tests in every supported development environment.
 * Testcontainers remains the preferred path when Docker is available; otherwise, the same
 * tests run against a locally managed PostgreSQL binary instead of being skipped.
 */
public final class RealPostgres {
    private static final Database DATABASE = startDatabase();

    private RealPostgres() {
    }

    public static String jdbcUrl() {
        return DATABASE.jdbcUrl();
    }

    public static String username() {
        return DATABASE.username();
    }

    public static String password() {
        return DATABASE.password();
    }

    private static Database startDatabase() {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17.6-alpine");
            container.start();
            registerShutdown(container);
            return new Database(
                    container.getJdbcUrl(),
                    container.getUsername(),
                    container.getPassword());
        }

        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
            registerShutdown(postgres);
            return new Database(
                    postgres.getJdbcUrl("postgres", "postgres"),
                    "postgres",
                    "");
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not start a real PostgreSQL test server with Docker or the embedded fallback",
                    exception);
        }
    }

    private static void registerShutdown(AutoCloseable database) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                database.close();
            } catch (Exception ignored) {
                // The process is already shutting down; there is no recovery action to take here.
            }
        }, "pantry-test-postgres-shutdown"));
    }

    private record Database(String jdbcUrl, String username, String password) {
    }
}
