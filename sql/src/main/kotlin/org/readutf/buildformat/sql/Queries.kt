package org.readutf.buildformat.sql

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertIgnoreAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class Queries(val database: Database) {

    init {
        transaction(database) {
            SchemaUtils.create(
                Tables.BuildMetaTable, Tables.BuildVersionTable, Tables.BuildSupportedFormats
            )
        }
    }

    fun buildExists(buildName: String): Boolean {
        return transaction(database) {
            Tables.BuildMetaTable.select { Tables.BuildMetaTable.name eq buildName }.count() > 0
        }
    }

    fun save(
        name: String,
        supportedFormats: List<String>,
        checksum: String
    ): Int? {
        return transaction {
            val metaId = Tables.BuildMetaTable.insertIgnoreAndGetId {
                it[this.name] = name
                it[this.createdAt] = System.currentTimeMillis()
            }

            metaId?.value
        }
    }

}