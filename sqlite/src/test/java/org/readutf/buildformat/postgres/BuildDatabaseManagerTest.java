package org.readutf.buildformat.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.readutf.buildformat.Build;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

class BuildDatabaseManagerTest {

    private DSLContext context;

    @BeforeEach
    public void beforeEach() {

        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl("jdbc:sqlite::memory:");
        config.setMaximumPoolSize(1);
        config.setDriverClassName("org.sqlite.JDBC");

        HikariDataSource hikariDataSource = new HikariDataSource(config);

        FluentConfiguration flywayConfig = Flyway.configure()
                .dataSource(hikariDataSource)
                .locations("classpath:db/migration");

        context = DSL.using(hikariDataSource, SQLDialect.SQLITE);

        Flyway load = flywayConfig.load();
        load.migrate();
    }

    @Test
    void saveBuild() {
        BuildDatabaseManager.saveBuild(context, "test1", "Test description", "test", List.of("1", "2"));
        BuildDatabaseManager.saveBuild(context, "test2", "Test description", "test", List.of("1", "2"));
        BuildDatabaseManager.saveBuild(context, "test2", "Test description", "test", List.of("1", "2"));
        BuildDatabaseManager.saveBuild(context, "test2", "Test description", "test", List.of("3", "4"));
    }

    @Test
    void saveBuildTest() {
        String name = "test1";
        String description = "Test description";
        LocalDateTime creationTime = LocalDateTime.now();
        String checksum = "test";
        List<String> supportedFormats = List.of("1", "2");

        BuildDatabaseManager.saveBuild(context, name, description, checksum, supportedFormats);
        Build test1 = BuildDatabaseManager.getBuild(context, name);

        System.out.println(test1);

        Assertions.assertNotNull(test1);
        Assertions.assertEquals(name, test1.name());
        Assertions.assertEquals(description, test1.description());
        Assertions.assertEquals(checksum, test1.checksum());
        Assertions.assertEquals(1, test1.version());
        Assertions.assertEquals(creationTime.truncatedTo(ChronoUnit.MILLIS), test1.creationTimestamp().truncatedTo(ChronoUnit.MILLIS));
        Assertions.assertEquals(supportedFormats, test1.formats());

    }

    @Test
    void saveSecondBuild() {
        String name = "test1";
        String description = "Test description";
        LocalDateTime creationTime = LocalDateTime.now().minusDays(1);
        String checksum = "test";
        List<String> supportedFormats = List.of("1", "2");

        BuildDatabaseManager.saveBuild(context, name, description, checksum, supportedFormats);

        String otherDesc = "Test 2 desc";
        String otherChecksum = "checksum";
        List<String> secondFormats = List.of("3");

        BuildDatabaseManager.saveBuild(context, name, otherDesc, otherChecksum, secondFormats);
        Build test1 = BuildDatabaseManager.getBuild(context, name);

        Assertions.assertNotNull(test1);
        Assertions.assertEquals(name, test1.name());
        Assertions.assertEquals(otherDesc, test1.description());
        Assertions.assertEquals(otherChecksum, test1.checksum());
        Assertions.assertEquals(2, test1.version());
        Assertions.assertEquals(secondFormats, test1.formats());

    }

    @Test
    void getLatestBuildsByFormat() {

        BuildDatabaseManager.saveBuild(context, "test1", "Test description", "test", List.of("1", "2"));
        BuildDatabaseManager.saveBuild(context, "test1", "Test description", "test", List.of("1", "2"));
        BuildDatabaseManager.saveBuild(context, "test1", "Test description", "test", List.of("1"));
        BuildDatabaseManager.saveBuild(context, "test1", "Test description", "test", List.of("1"));
        BuildDatabaseManager.getBuildsByFormat(context, "2");
    }
}
