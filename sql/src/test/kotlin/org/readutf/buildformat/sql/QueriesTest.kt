package org.readutf.buildformat.sql

import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.DriverManager

class QueriesTest {

    val queries: Queries

    init {
        val sqlitePath = "jdbc:sqlite::memory:"
        val keepAliveConnection = DriverManager.getConnection(sqlitePath)
        val db = Database.connect(sqlitePath, "org.sqlite.JDBC")
        queries = Queries(db)
    }

    @Test
    fun testBuildExists() {

        queries.save("test",listOf("format1", "format2"), "checksum123")

    }

}