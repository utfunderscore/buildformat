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
import org.readutf.buildformat.settings.BuildMetadata;
import org.readutf.buildformat.settings.BuildSetting;
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
                .locations("classpath:db/migration/");

        Flyway load = flywayConfig.load();
        load.migrate();


        context = DSL.using(hikariDataSource, SQLDialect.POSTGRES);
        metaStore = new SQLBuildMetaStore(context);
    }

    @Test
    void saveBuild() throws Exception {
        BuildMetadata metadata = new BuildMetadata(Map.of(
                "position", new BuildSetting<>(new Position(10, 20, 30)),
                "cuboid", new BuildSetting<>(new Cuboid(new Position(0, 0, 0), new Position(10, 10, 10))),
                "positions", new BuildSetting<>(List.of(new Position(1, 2, 3), new Position(4, 5, 6))),
                "count", new BuildSetting<>(42),
                "description", new BuildSetting<>("Test build")
        ));
        
        int i = metaStore.saveBuild("test1", 12345, "2", metadata);
        metaStore.saveBuild("test2", 12345, "2", metadata);
        metaStore.saveBuild("test2", 12345, "3", metadata);
        int last = metaStore.saveBuild("test2", 12345, "3", metadata);

        Assertions.assertEquals(1, i);
        Assertions.assertEquals(3, last);
    }

    @Test
    void saveBuildTest() throws Exception {
        String name = "test1";
        LocalDateTime creationTime = LocalDateTime.now();
        int checksum = 67890;
        String supportedFormats = "2";
        
        BuildMetadata metadata = new BuildMetadata(Map.of(
                "position", new BuildSetting<>(new Position(10, 20, 30)),
                "cuboid", new BuildSetting<>(new Cuboid(new Position(0, 0, 0), new Position(10, 10, 10))),
                "positions", new BuildSetting<>(List.of(new Position(1, 2, 3), new Position(4, 5, 6))),
                "count", new BuildSetting<>(42),
                "description", new BuildSetting<>("Test build")
        ));

        metaStore.saveBuild(name, checksum, supportedFormats, metadata);
        BuildMeta test1 = metaStore.getBuild(name);

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
        int checksum = 11111;
        String supportedFormat = "2";
        
        BuildMetadata metadata = new BuildMetadata(Map.of(
                "position", new BuildSetting<>(new Position(5, 10, 15)),
                "cuboid", new BuildSetting<>(new Cuboid(new Position(0, 0, 0), new Position(5, 5, 5))),
                "positions", new BuildSetting<>(List.of(new Position(1, 1, 1))),
                "count", new BuildSetting<>(10),
                "description", new BuildSetting<>(description)
        ));

        metaStore.saveBuild(name, checksum, supportedFormat, metadata);

        String otherDesc = "Test 2 desc";
        int otherChecksum = 22222;
        String secondFormat = "3";
        
        BuildMetadata metadata2 = new BuildMetadata(Map.of(
                "position", new BuildSetting<>(new Position(15, 25, 35)),
                "cuboid", new BuildSetting<>(new Cuboid(new Position(0, 0, 0), new Position(20, 20, 20))),
                "positions", new BuildSetting<>(List.of(new Position(2, 2, 2), new Position(3, 3, 3))),
                "count", new BuildSetting<>(20),
                "description", new BuildSetting<>(otherDesc)
        ));

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
        BuildMetadata metadata = new BuildMetadata(Map.of(
                "position", new BuildSetting<>(new Position(10, 20, 30)),
                "cuboid", new BuildSetting<>(new Cuboid(new Position(0, 0, 0), new Position(10, 10, 10))),
                "positions", new BuildSetting<>(List.of(new Position(1, 2, 3))),
                "count", new BuildSetting<>(100),
                "name", new BuildSetting<>("test")
        ));

        metaStore.saveBuild("test1", 33333, "2", metadata);
        metaStore.saveBuild("test1", 33333, "2", metadata);
        metaStore.saveBuild("test2", 44444, "1", metadata);
        metaStore.saveBuild("test2", 55555, "2", metadata);
        metaStore.saveBuild("test2", 66666, "3", metadata);

        Assertions.assertEquals(2, metaStore.getBuildsByFormat("2").size());
    }

    @Test
    void getBuildByVersion() throws Exception {
        BuildMetadata metadata1 = new BuildMetadata(Map.of(
                "position", new BuildSetting<>(new Position(1, 2, 3)),
                "cuboid", new BuildSetting<>(new Cuboid(new Position(0, 0, 0), new Position(5, 5, 5))),
                "positions", new BuildSetting<>(List.of(new Position(1, 1, 1))),
                "version", new BuildSetting<>(1),
                "type", new BuildSetting<>("test")
        ));
        
        BuildMetadata metadata2 = new BuildMetadata(Map.of(
                "position", new BuildSetting<>(new Position(4, 5, 6)),
                "cuboid", new BuildSetting<>(new Cuboid(new Position(10, 10, 10), new Position(20, 20, 20))),
                "positions", new BuildSetting<>(List.of(new Position(2, 2, 2), new Position(3, 3, 3))),
                "version", new BuildSetting<>(2),
                "type", new BuildSetting<>("different")
        ));
        
        BuildMetadata metadata3 = new BuildMetadata(Map.of(
                "position", new BuildSetting<>(new Position(7, 8, 9)),
                "cuboid", new BuildSetting<>(new Cuboid(new Position(20, 20, 20), new Position(30, 30, 30))),
                "positions", new BuildSetting<>(List.of(new Position(4, 4, 4))),
                "version", new BuildSetting<>(3),
                "type", new BuildSetting<>("test")
        ));

        metaStore.saveBuild("test2", 77777, "1", metadata1);
        metaStore.saveBuild("test2", 88888, "2", metadata2);
        metaStore.saveBuild("test2", 99999, "3", metadata3);

        BuildMeta build = metaStore.getBuild("test2", 2);
        Assertions.assertNotNull(build);
        Assertions.assertEquals(88888, build.checksum());
        Assertions.assertEquals(2, build.version());
    }

}
