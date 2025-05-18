package org.readutf.buildstore;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresDatabaseManager {

    private final Logger logger = LoggerFactory.getLogger(PostgresDatabaseManager.class);
    private final @NotNull HikariDataSource dataSource;

    public PostgresDatabaseManager(@NotNull HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected Connection getConnection() throws BuildFormatException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new BuildFormatException("Failed to connect to database", e);
        }
    }

    protected void run(ConsumerRunner consumer) throws BuildFormatException {
        try (Connection connect = getConnection()) {
            try {
                connect.setAutoCommit(false);
                DSLContext context = DSL.using(connect, SQLDialect.POSTGRES);

                consumer.run(context);
            } catch (Exception e) {
                throw new BuildFormatException("Failed to get build meta", e);
            } finally {
                connect.setAutoCommit(true);
                connect.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T runReturning(Runner<T> function) throws BuildFormatException {
        try (Connection connect = getConnection()) {
            try {
                connect.setAutoCommit(false);
                DSLContext context = DSL.using(connect, SQLDialect.POSTGRES);
                return function.run(context);
            } catch (Exception e) {
                logger.error("Failed to get build meta", e);
                throw new BuildFormatException("A database error occurred", e);
            } finally {
                connect.commit();
                connect.setAutoCommit(true);
                connect.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Runner<T> {
        T run(DSLContext context) throws BuildFormatException;
    }

    public interface ConsumerRunner {
        void run(DSLContext context) throws BuildFormatException;
    }

}
