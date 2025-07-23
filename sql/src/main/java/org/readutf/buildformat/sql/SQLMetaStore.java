package org.readutf.buildformat.sql;

import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.common.meta.BuildMetaStore;

public class SQLMetaStore {

    public static BuildMetaStore createMetaStore(@NotNull HikariDataSource dataSource) {
        return new ExposedMetaStore(dataSource);
    }
}
