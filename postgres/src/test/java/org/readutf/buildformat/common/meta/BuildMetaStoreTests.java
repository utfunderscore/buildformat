package org.readutf.buildformat.common.meta;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;
import org.readutf.buildstore.PostgresDatabaseManager;
import org.readutf.buildstore.PostgresMetaStore;

@TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
public class BuildMetaStoreTests {

    private PostgresMetaStore metaStore;

    @BeforeAll
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


        metaStore = new PostgresMetaStore(new PostgresDatabaseManager(ds));
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

    @Test
    public void get_by_name_success() throws BuildFormatException {
        BuildMeta getByNameSuccess = metaStore.create("get_by_name_success", "Description!");

        assertEquals(getByNameSuccess, metaStore.getByName("get_by_name_success"));
    }

    @Test
    public void get_by_name_not_found() throws BuildFormatException {
        metaStore.create("get_by_name_not_found", "Description!");

        assertNull(metaStore.getByName("non_existent"));
    }

    @Test
    public void setFormats() throws BuildFormatException {


        metaStore.create("setFormats", "Description!");
        List<BuildFormatChecksum> formats = List.of(
                new BuildFormatChecksum("test", "test".getBytes(StandardCharsets.UTF_8)),
                new BuildFormatChecksum("test2", "test2".getBytes(StandardCharsets.UTF_8))
        );
        metaStore.setFormats("setFormats", formats);

        BuildMeta meta = metaStore.getByName("setFormats");
        assertNotNull(meta);

        List<BuildFormatChecksum> sortedExpected = formats.stream().sorted(Comparator.comparingInt(BuildFormatChecksum::hashCode)).toList();
        List<BuildFormatChecksum> sortedResult = meta.formats().stream().sorted(Comparator.comparingInt(BuildFormatChecksum::hashCode)).toList();

        assertEquals(sortedExpected, sortedResult);
    }

    @Test
    void setFormatsOverride() throws BuildFormatException {
        metaStore.create("setFormatsOverride", "Description!");

        List<BuildFormatChecksum> original = List.of(
                new BuildFormatChecksum("test", "test".getBytes(StandardCharsets.UTF_8)),
                new BuildFormatChecksum("test2", "test2".getBytes(StandardCharsets.UTF_8))
        );

        metaStore.setFormats("setFormatsOverride", original);

        List<BuildFormatChecksum> formats = List.of(
                new BuildFormatChecksum("test", "test".getBytes(StandardCharsets.UTF_8)),
                new BuildFormatChecksum("test2", "test2".getBytes(StandardCharsets.UTF_8))
        );

        metaStore.setFormats("setFormatsOverride", formats);

        BuildMeta meta = metaStore.getByName("setFormatsOverride");
        assertNotNull(meta);

        List<BuildFormatChecksum> sortedExpected = formats.stream().sorted(Comparator.comparingInt(BuildFormatChecksum::hashCode)).toList();
        List<BuildFormatChecksum> sortedResult = meta.formats().stream().sorted(Comparator.comparingInt(BuildFormatChecksum::hashCode)).toList();

        assertEquals(sortedExpected, sortedResult);
    }

    @Test
    void getByFormat() throws BuildFormatException {
        metaStore.create("get_by_format1", "Description!");
        metaStore.create("get_by_format2", "Description!");

        List<BuildFormatChecksum> formats = List.of(new BuildFormatChecksum("get_by_format", "test".getBytes(StandardCharsets.UTF_8)));
        List<BuildFormatChecksum> formats2 = List.of(new BuildFormatChecksum("get_by_format", "test".getBytes(StandardCharsets.UTF_8)));

        metaStore.setFormats("get_by_format1", formats);
        metaStore.setFormats("get_by_format2", formats2);



    }


}
