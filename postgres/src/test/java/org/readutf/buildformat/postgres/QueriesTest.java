package org.readutf.buildformat.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class QueriesTest {

    private DSLContext context;

    @BeforeEach
    public void beforeEach() {

        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl("jdbc:postgresql://localhost:5433/testdb");
        config.setPassword("mysecretpassword");
        config.setUsername("postgres");

        HikariDataSource hikariDataSource = new HikariDataSource(config);

        context = DSL.using(hikariDataSource, SQLDialect.POSTGRES);
    }


    @Test
    void saveBuild() {

        Queries.saveBuild(context, "test1", "Test description", LocalDateTime.now(), "test", List.of("1", "2"));
        Queries.saveBuild(context, "test2", "Test description", LocalDateTime.now(), "test", List.of("1", "2"));
        Queries.saveBuild(context, "test2", "Test description", LocalDateTime.now(), "test", List.of("1", "2"));
        Queries.saveBuild(context, "test2", "Test description", LocalDateTime.now(), "test", List.of("3", "4"));

    }

    @Test
    void getBuild() {
        System.out.println(Queries.getBuild(context, "test1"));
    }

    @Test
    void getBuildsByFormat() {

        Queries.getLatestSupporting(context, "2");

    }



}