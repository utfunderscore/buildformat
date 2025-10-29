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
import org.readutf.buildformat.BuildMeta;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

class SQLiteBuildMetaStoreTest {

    private DSLContext context;
    private SQLBuildMetaStore metaStore;

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
                .locations("classpath:db/migration/sqlite");

        context = DSL.using(hikariDataSource, SQLDialect.SQLITE);

        Flyway load = flywayConfig.load();
        load.migrate();
        
        metaStore = new SQLBuildMetaStore(context);
    }

    @Test
    void saveBuild() {
        int i = metaStore.saveBuild("test1", "test", "2");
        metaStore.saveBuild("test2", "test", "2");
        metaStore.saveBuild("test2", "test", "3");
        int last = metaStore.saveBuild("test2", "test", "3");

        Assertions.assertEquals(1, i);
        Assertions.assertEquals(3, last);
    }

    @Test
    void saveBuildTest() {
        String name = "test1";
        String description = "Test description";
        LocalDateTime creationTime = LocalDateTime.now();
        String checksum = "test";
        String supportedFormats = "2";

        metaStore.saveBuild(name, checksum, supportedFormats);
        BuildMeta test1 = metaStore.getBuild(name);

        System.out.println(test1);

        Assertions.assertNotNull(test1);
        Assertions.assertEquals(name, test1.name());
        Assertions.assertEquals(checksum, test1.checksum());
        Assertions.assertEquals(1, test1.version());
        Assertions.assertEquals(creationTime.truncatedTo(ChronoUnit.SECONDS), test1.creationTimestamp().truncatedTo(ChronoUnit.SECONDS));
        Assertions.assertEquals(supportedFormats, test1.format());

    }

    @Test
    void saveSecondBuild() {
        String name = "test1";
        String description = "Test description";
        LocalDateTime creationTime = LocalDateTime.now().minusDays(1);
        String checksum = "test";
        String supportedFormat = "2";

        metaStore.saveBuild(name, checksum, supportedFormat);

        String otherDesc = "Test 2 desc";
        String otherChecksum = "checksum";
        String secondFormat = "3";

        metaStore.saveBuild(name, otherChecksum, secondFormat);
        BuildMeta test1 = metaStore.getBuild(name);

        Assertions.assertNotNull(test1);
        Assertions.assertEquals(name, test1.name());
        Assertions.assertEquals(otherChecksum, test1.checksum());
        Assertions.assertEquals(2, test1.version());
        Assertions.assertEquals(secondFormat, test1.format());

    }

    @Test
    void getLatestBuildsByFormat() {

        metaStore.saveBuild("test1", "test", "2");
        metaStore.saveBuild("test1", "test", "2");
        metaStore.saveBuild("test2", "test", "1");
        metaStore.saveBuild("test2", "test", "2");
        metaStore.saveBuild("test2", "test", "3");

        Assertions.assertEquals(2, metaStore.getBuildsByFormat("2").size());
    }

    @Test
    void getBuildByVersion() {

        metaStore.saveBuild("test2", "test", "1");
        metaStore.saveBuild("test2", "different", "2");
        metaStore.saveBuild("test2", "test", "3");

        BuildMeta build = metaStore.getBuild("test2", 2);
        Assertions.assertNotNull(build);
        Assertions.assertEquals("different", build.checksum());
        Assertions.assertEquals(2, build.version());
    }

}
