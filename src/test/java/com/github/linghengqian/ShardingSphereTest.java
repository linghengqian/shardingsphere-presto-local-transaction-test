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
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@SuppressWarnings({"SqlNoDataSourceInspection", "resource"})
@Testcontainers
public class ShardingSphereTest {
    private final String systemPropKeyPrefix = "fixture.test.yaml.database.presto.";
    private String baseJdbcUrl;

    @Container
    private final GenericContainer<?> container = new GenericContainer<>("prestodb/presto:0.292")
            .withExposedPorts(8080)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("iceberg.properties"),
                    "/opt/presto-server/etc/catalog/iceberg.properties")
            .waitingFor(Wait.forHttp("/v1/info/state").forPort(8080).forResponsePredicate("\"ACTIVE\""::equals));

    @Test
    void assertShardingInLocalTransactions() throws SQLException {
        assertThat(System.getProperty(systemPropKeyPrefix + "ds0.jdbc-url"), is(nullValue()));
        assertThat(System.getProperty(systemPropKeyPrefix + "ds1.jdbc-url"), is(nullValue()));
        assertThat(System.getProperty(systemPropKeyPrefix + "ds2.jdbc-url"), is(nullValue()));
        baseJdbcUrl = "jdbc:presto://localhost:" + container.getMappedPort(8080) + "/iceberg";
        DataSource logicDataSource = createDataSource();
        this.assertRollbackWithTransactions(logicDataSource);
        System.clearProperty(systemPropKeyPrefix + "ds0.jdbc-url");
        System.clearProperty(systemPropKeyPrefix + "ds1.jdbc-url");
        System.clearProperty(systemPropKeyPrefix + "ds2.jdbc-url");
    }

    private DataSource createDataSource() throws SQLException {
        Awaitility.await().atMost(Duration.ofMinutes(1L)).ignoreExceptions().until(() -> {
            DriverManager.getConnection(baseJdbcUrl, "test", null).close();
            return true;
        });
        try (
                Connection con = DriverManager.getConnection(baseJdbcUrl, "test", null);
                Statement stmt = con.createStatement()) {
            stmt.execute("CREATE SCHEMA iceberg.demo_ds_0");
            stmt.execute("CREATE SCHEMA iceberg.demo_ds_1");
            stmt.execute("CREATE SCHEMA iceberg.demo_ds_2");
        }
        Stream.of("demo_ds_0", "demo_ds_1", "demo_ds_2").forEach(schemaName -> {
            try (
                    Connection con = DriverManager.getConnection(baseJdbcUrl + "/" + schemaName, "test", null);
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
        });
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.apache.shardingsphere.driver.ShardingSphereDriver");
        config.setJdbcUrl("jdbc:shardingsphere:classpath:demo.yaml?placeholder-type=system_props");
        System.setProperty(systemPropKeyPrefix + "ds0.jdbc-url", baseJdbcUrl + "/demo_ds_0");
        System.setProperty(systemPropKeyPrefix + "ds1.jdbc-url", baseJdbcUrl + "/demo_ds_1");
        System.setProperty(systemPropKeyPrefix + "ds2.jdbc-url", baseJdbcUrl + "/demo_ds_2");
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
