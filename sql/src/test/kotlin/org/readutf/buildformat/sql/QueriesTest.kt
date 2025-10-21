package org.readutf.buildformat.sql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QueriesTest {

    lateinit var cfg: HikariConfig
    lateinit var dataSource: HikariDataSource
    lateinit var database: Database

    @BeforeEach
    fun beforeEach() {
        cfg = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite::memory:"
            maximumPoolSize = 6
        }
        dataSource = HikariDataSource(cfg)
        database = Database.connect(dataSource)
    }

    @Test
    fun save() {
        val queries = Queries(database)

        queries.save("test", listOf("testformat"), "test")
    }

}