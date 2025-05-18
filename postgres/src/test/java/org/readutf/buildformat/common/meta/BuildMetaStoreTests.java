package org.readutf.buildformat.common.meta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildstore.PostgresMetaStore;

public class BuildMetaStoreTests {

    private PostgresMetaStore metaStore;

    @BeforeEach
    void setUp() {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/example_db");
        config.setUsername("postgres");
        config.setPassword("password");
        config.addDataSourceProperty("cachePrepStmts", "true");

        HikariDataSource ds = new HikariDataSource(config);

        Flyway flyway = Flyway.configure()
                .cleanDisabled(false)
                .dataSource(ds)
                .load();

        flyway.clean();

        flyway.migrate();


        metaStore = new PostgresMetaStore(ds);
    }

    @Test
    public void create_success() throws BuildFormatException {
        metaStore.create("create_success", "Description!");
    }

    @Test
    public void create_duplicate_failure() throws BuildFormatException {
        metaStore.create("create_duplicate", "Description!");
        assertThrows(BuildFormatException.class, () -> metaStore.create("create_duplicate", "Description!"));
    }

}
