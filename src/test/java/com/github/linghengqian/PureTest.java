package com.github.linghengqian;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.awaitility.Awaitility;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"resource", "SqlNoDataSourceInspection"})
@Testcontainers
public class PureTest {
    private String baseJdbcUrl;
    @Container
    private final GenericContainer<?> container = new GenericContainer<>("prestodb/presto:0.292")
            .withExposedPorts(8080)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("iceberg.properties"),
                    "/opt/presto-server/etc/catalog/iceberg.properties")
            .waitingFor(Wait.forHttp("/v1/info/state").forPort(8080).forResponsePredicate("\"ACTIVE\""::equals));

    @Test
    void assertLocalTransactions() throws SQLException {
        baseJdbcUrl = "jdbc:presto://localhost:" + container.getMappedPort(8080) + "/iceberg";
        DataSource logicDataSource = createDataSource();
        this.assertRollbackWithTransactions(logicDataSource);
    }

    private DataSource createDataSource() throws SQLException {
        Awaitility.await().atMost(Duration.ofMinutes(1L)).ignoreExceptions().until(() -> {
            DriverManager.getConnection(baseJdbcUrl, "test", null).close();
            return true;
        });
        try (Connection con = DriverManager.getConnection(baseJdbcUrl, "test", null);
             Statement stmt = con.createStatement()) {
            stmt.execute("CREATE SCHEMA iceberg.demo_ds_0");
        }
        try (Connection con = DriverManager.getConnection(baseJdbcUrl + "/demo_ds_0", "test", null);
             Statement stmt = con.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS t_order (
                        order_id BIGINT NOT NULL,
                        order_type INTEGER,
                        user_id INTEGER NOT NULL,
                        address_id BIGINT NOT NULL,
                        status VARCHAR(50)
                    )""");
            stmt.execute("truncate table t_order");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("com.facebook.presto.jdbc.PrestoDriver");
        config.setJdbcUrl(baseJdbcUrl + "/demo_ds_0");
        config.setUsername("test");
        return new HikariDataSource(config);
    }

    private void assertRollbackWithTransactions(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);
                conn.createStatement().executeUpdate("INSERT INTO t_order (user_id, order_type, address_id, status) VALUES (2024, 1, 1, 'INSERT_TEST')");
                conn.createStatement().executeUpdate("INSERT INTO t_order_does_not_exist (test_id_does_not_exist) VALUES (2024)");
                conn.commit();
            } catch (SQLException ignored) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        }
        try (Connection conn = dataSource.getConnection()) {
            ResultSet resultSet = conn.createStatement().executeQuery("SELECT * FROM t_order WHERE user_id = 2024");
            assertThat(resultSet.next(), CoreMatchers.is(false));
        }
        try (Connection conn = dataSource.getConnection()) {
            try {
                conn.setAutoCommit(false);
                conn.createStatement().executeUpdate("INSERT INTO t_order (user_id, order_type, address_id, status) VALUES (2025, 1, 1, 'INSERT_TEST')");
                conn.commit();
            } catch (SQLException ignored) {
                conn.rollback();
            } finally {
                conn.setAutoCommit(true);
            }
        }
        try (Connection conn = dataSource.getConnection()) {
            ResultSet resultSet = conn.createStatement().executeQuery("SELECT * FROM t_order WHERE user_id = 2025");
            assertThat(resultSet.next(), CoreMatchers.is(true));
        }
    }
}
