package org.readutf.buildformat.sql

import org.jetbrains.exposed.dao.IntIdTable

object Tables {

    object BuildMetaTable : IntIdTable("build_meta") {
        val name = varchar("build_name", 16)
        val createdAt = long("created_at")

        init {
            index(true, name)
        }
    }

    object BuildVersionTable : IntIdTable("build_version") {
        val metaId = reference("build_meta_id", BuildMetaTable)
        val description = varchar("description", 128)
        val versionNumber = integer("version_number")
        val checksum = varchar("checksum", 64)

        init {
            index(true, metaId, versionNumber)
        }
    }

    object BuildSupportedFormats : IntIdTable("build_supported_formats") {
        val versionId = reference("build_version_id", BuildVersionTable)
        val formatName = varchar("format_name", 32)

        init {
            index(true, versionId, formatName)
        }
    }
}