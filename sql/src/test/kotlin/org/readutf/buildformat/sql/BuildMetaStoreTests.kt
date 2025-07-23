package org.readutf.buildformat.sql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.readutf.buildformat.common.exception.BuildFormatException
import org.readutf.buildformat.common.format.BuildFormatChecksum
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class BuildMetaStoreTests {

    private val postgres = PostgreSQLContainer("postgres:16")
        .withDatabaseName("builds")

    private lateinit var metaStore: ExposedMetaStore

    @BeforeAll
    fun setUp() {
        // Make sure the container is started
        postgres.start()

        val config = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            driverClassName = "org.postgresql.Driver"
            addDataSourceProperty("cachePrepStmts", "true")
        }

        val ds = HikariDataSource(config)
        metaStore = ExposedMetaStore(Database.connect(ds))
    }

    @Test
    fun create_success() {
        metaStore.create("create_success", "Description!")
    }

    @Test
    fun create_duplicate_failure() {
        metaStore.create("create_duplicate", "Description!")
        assertThrows(BuildFormatException::class.java) { 
            metaStore.create("create_duplicate", "Description!") 
        }
    }

    @Test
    fun get_by_name_success() {
        val getByNameSuccess = metaStore.create("get_by_name_success", "Description!")
        assertEquals(getByNameSuccess, metaStore.getByName("get_by_name_success"))
    }

    @Test
    fun get_by_name_not_found() {
        metaStore.create("get_by_name_not_found", "Description!")
        assertNull(metaStore.getByName("non_existent"))
    }

    @Test
    fun update() {
        metaStore.create("setFormats", "Description!")
        val formats = listOf(
            BuildFormatChecksum("test", "test".toByteArray(StandardCharsets.UTF_8)),
            BuildFormatChecksum("test2", "test2".toByteArray(StandardCharsets.UTF_8))
        )
        metaStore.update("setFormats", formats)

        val meta = metaStore.getByName("setFormats")
        assertNotNull(meta)

        val sortedExpected = formats.sortedBy { it.hashCode() }
        val sortedResult = meta!!.formats().sortedBy { it.hashCode() }

        assertEquals(sortedExpected, sortedResult)
    }

    @Test
    fun updateOverride() {
        metaStore.create("setFormatsOverride", "Description!")

        val original = listOf(
            BuildFormatChecksum("test", "test".toByteArray(StandardCharsets.UTF_8)),
            BuildFormatChecksum("test2", "test2".toByteArray(StandardCharsets.UTF_8))
        )

        metaStore.update("setFormatsOverride", original)

        val formats = listOf(
            BuildFormatChecksum("test", "test".toByteArray(StandardCharsets.UTF_8)),
            BuildFormatChecksum("test2", "test2".toByteArray(StandardCharsets.UTF_8))
        )

        metaStore.update("setFormatsOverride", formats)

        val meta = metaStore.getByName("setFormatsOverride")
        assertNotNull(meta)

        val sortedExpected = formats.sortedBy { it.hashCode() }
        val sortedResult = meta!!.formats().sortedBy { it.hashCode() }

        assertEquals(sortedExpected, sortedResult)
    }

    @Test
    fun getByFormat() {
//        metaStore.create("get_by_format1", "Description!")
//        metaStore.create("get_by_format2", "Description!")
//
//        val formats = listOf(BuildFormatChecksum("get_by_format", "test".toByteArray(StandardCharsets.UTF_8)))
//        val formats2 = listOf(BuildFormatChecksum("get_by_format", "test".toByteArray(StandardCharsets.UTF_8)))
//
//        metaStore.update("get_by_format1", formats)
//        metaStore.update("get_by_format2", formats2)
//
//        metaStore.getBuildsByFormat("get_by_format").forEach { (name, checksum) ->
//            assertTrue(name == "get_by_format1" || name == "get_by_format2")
//            assertEquals("get_by_format", checksum.name())
//        }

        val tnttag = metaStore.getBuildsByFormat("tnttag")

        println(tnttag)
    }
}