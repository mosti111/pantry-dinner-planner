package com.pantry;

import static org.assertj.core.api.Assertions.assertThat;

import com.pantry.support.RealPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class PostgresMigrationTest {
    @Test
    void migrationCreatesTrigramExtensionAndCoreTables() throws Exception {
        Flyway.configure()
                .dataSource(RealPostgres.jdbcUrl(), RealPostgres.username(), RealPostgres.password())
                .load().migrate();
        try (Connection connection = DriverManager.getConnection(
                RealPostgres.jdbcUrl(), RealPostgres.username(), RealPostgres.password());
             var statement = connection.createStatement();
             var result = statement.executeQuery("select count(*) from information_schema.tables where table_name in "
                     + "('guest_session', 'account', 'meal_plan', 'cart_attempt', 'ingredient', 'recipe', 'retailer', 'product', 'offer_snapshot', 'plan_transition', 'ai_proposal', 'integration_call')")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(12);
        }
    }
}
