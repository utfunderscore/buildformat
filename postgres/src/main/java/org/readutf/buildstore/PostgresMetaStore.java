package org.readutf.buildstore;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;
import org.readutf.buildformat.common.meta.BuildMeta;
import org.readutf.buildformat.common.meta.BuildMetaStore;
import org.readutf.buildstore.generated.Tables;
import org.readutf.buildstore.generated.tables.records.BuildmetaRecord;

public class PostgresMetaStore implements BuildMetaStore {

    private final @NotNull HikariDataSource dataSource;

    public PostgresMetaStore(@NotNull HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public @NotNull BuildMeta create(String name, String description) throws BuildFormatException {

        try (Connection connection = getConnection()) {
            try {
                DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
                BuildmetaRecord buildmetaRecord = context.newRecord(Tables.BUILDMETA);
                buildmetaRecord.setName(name);
                buildmetaRecord.setDescription(description);

                buildmetaRecord.store();

                return new BuildMeta(name, description, Collections.emptyList(), Collections.emptyList());
            } catch (Exception e) {
                throw new BuildFormatException("Failed to create build meta", e);
            }
        } catch (SQLException e) {
            throw new BuildFormatException("Failed to close connection", e);
        }
    }

    @Override
    public @Nullable BuildMeta getByName(String name) throws BuildFormatException {
        try(Connection connect = getConnection()) {
            try {
                DSLContext context = DSL.using(connect, SQLDialect.POSTGRES);
                BuildmetaRecord record = context.fetchOne(Tables.BUILDMETA, Tables.BUILDMETA.NAME.eq(name));

                if(record == null) return null;

                return new BuildMeta(record.getName(), record.getDescription(), Collections.emptyList(), Collections.emptyList());
            } catch (Exception e) {
                throw new BuildFormatException("Failed to get build meta", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setFormats(String name, List<BuildFormatChecksum> formats) throws BuildFormatException {

    }

    public Connection getConnection() throws BuildFormatException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new BuildFormatException("Failed to connect to database", e);
        }
    }

}
