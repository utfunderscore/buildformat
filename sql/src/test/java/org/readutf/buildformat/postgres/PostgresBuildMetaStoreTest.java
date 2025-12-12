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
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

class PostgresBuildMetaStoreTest {

    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:17-alpine"
    );

    private DSLContext context;
    private SQLBuildMetaStore metaStore;

    @BeforeEach
    public void beforeEach() {

        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);

        // start postgres testcontainer and use its connection info
        postgres.start();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(20);
        config.setDriverClassName("org.postgresql.Driver");

        HikariDataSource hikariDataSource = new HikariDataSource(config);

        FluentConfiguration flywayConfig = Flyway.configure()
                .dataSource(hikariDataSource)
                .locations("classpath:db/migration/postgres");

        Flyway load = flywayConfig.load();
        load.migrate();


        context = DSL.using(hikariDataSource, SQLDialect.POSTGRES);
        metaStore = new SQLBuildMetaStore(context);
    }

    @Test
    void saveBuild() throws Exception {
        Map<String, Object> metadata = Map.of(
                "position", new Position(10, 20, 30),
                "cuboid", new Cuboid(new Position(0, 0, 0), new Position(10, 10, 10)),
                "positions", List.of(new Position(1, 2, 3), new Position(4, 5, 6)),
                "count", 42,
                "description", "Test build"
        );
        
        int i = metaStore.saveBuild("test1", "test", "2", metadata);
        metaStore.saveBuild("test2", "test", "2", metadata);
        metaStore.saveBuild("test2", "test", "3", metadata);
        int last = metaStore.saveBuild("test2", "test", "3", metadata);

        Assertions.assertEquals(1, i);
        Assertions.assertEquals(3, last);
    }

    @Test
    void saveBuildTest() throws Exception {
        String name = "test1";
        LocalDateTime creationTime = LocalDateTime.now();
        String checksum = "test";
        String supportedFormats = "2";
        
        Map<String, Object> metadata = Map.of(
                "position", new Position(10, 20, 30),
                "cuboid", new Cuboid(new Position(0, 0, 0), new Position(10, 10, 10)),
                "positions", List.of(new Position(1, 2, 3), new Position(4, 5, 6)),
                "count", 42,
                "description", "Test build"
        );

        metaStore.saveBuild(name, checksum, supportedFormats, metadata);
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
    void saveSecondBuild() throws Exception {
        String name = "test1";
        String description = "Test description";
        LocalDateTime creationTime = LocalDateTime.now().minusDays(1);
        String checksum = "test";
        String supportedFormat = "2";
        
        Map<String, Object> metadata = Map.of(
                "position", new Position(5, 10, 15),
                "cuboid", new Cuboid(new Position(0, 0, 0), new Position(5, 5, 5)),
                "positions", List.of(new Position(1, 1, 1)),
                "count", 10,
                "description", description
        );

        metaStore.saveBuild(name, checksum, supportedFormat, metadata);

        String otherDesc = "Test 2 desc";
        String otherChecksum = "checksum";
        String secondFormat = "3";
        
        Map<String, Object> metadata2 = Map.of(
                "position", new Position(15, 25, 35),
                "cuboid", new Cuboid(new Position(0, 0, 0), new Position(20, 20, 20)),
                "positions", List.of(new Position(2, 2, 2), new Position(3, 3, 3)),
                "count", 20,
                "description", otherDesc
        );

        metaStore.saveBuild(name, otherChecksum, secondFormat, metadata2);
        BuildMeta test1 = metaStore.getBuild(name);

        Assertions.assertNotNull(test1);
        Assertions.assertEquals(name, test1.name());
        Assertions.assertEquals(otherChecksum, test1.checksum());
        Assertions.assertEquals(2, test1.version());
        Assertions.assertEquals(secondFormat, test1.format());

    }

    @Test
    void getLatestBuildsByFormat() throws Exception {
        Map<String, Object> metadata = Map.of(
                "position", new Position(10, 20, 30),
                "cuboid", new Cuboid(new Position(0, 0, 0), new Position(10, 10, 10)),
                "positions", List.of(new Position(1, 2, 3)),
                "count", 100,
                "name", "test"
        );

        metaStore.saveBuild("test1", "test", "2", metadata);
        metaStore.saveBuild("test1", "test", "2", metadata);
        metaStore.saveBuild("test2", "test", "1", metadata);
        metaStore.saveBuild("test2", "test", "2", metadata);
        metaStore.saveBuild("test2", "test", "3", metadata);

        Assertions.assertEquals(2, metaStore.getBuildsByFormat("2").size());
    }

    @Test
    void getBuildByVersion() throws Exception {
        Map<String, Object> metadata1 = Map.of(
                "position", new Position(1, 2, 3),
                "cuboid", new Cuboid(new Position(0, 0, 0), new Position(5, 5, 5)),
                "positions", List.of(new Position(1, 1, 1)),
                "version", 1,
                "type", "test"
        );
        
        Map<String, Object> metadata2 = Map.of(
                "position", new Position(4, 5, 6),
                "cuboid", new Cuboid(new Position(10, 10, 10), new Position(20, 20, 20)),
                "positions", List.of(new Position(2, 2, 2), new Position(3, 3, 3)),
                "version", 2,
                "type", "different"
        );
        
        Map<String, Object> metadata3 = Map.of(
                "position", new Position(7, 8, 9),
                "cuboid", new Cuboid(new Position(20, 20, 20), new Position(30, 30, 30)),
                "positions", List.of(new Position(4, 4, 4)),
                "version", 3,
                "type", "test"
        );

        metaStore.saveBuild("test2", "test", "1", metadata1);
        metaStore.saveBuild("test2", "different", "2", metadata2);
        metaStore.saveBuild("test2", "test", "3", metadata3);

        BuildMeta build = metaStore.getBuild("test2", 2);
        Assertions.assertNotNull(build);
        Assertions.assertEquals("different", build.checksum());
        Assertions.assertEquals(2, build.version());
    }

}
