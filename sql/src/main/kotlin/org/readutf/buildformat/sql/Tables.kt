package org.readutf.buildformat.sql

import org.jetbrains.exposed.dao.id.IntIdTable


object Tables {
    object BuildMeta : IntIdTable("buildmeta") {
        val name = varchar("name", 32).uniqueIndex("UK_build_meta_name")
        val description = varchar("description", 128)
    }

    object BuildMetaFormat : IntIdTable("buildmeta_format") {
        val buildMeta = reference("buildmeta_id", BuildMeta)
        val name = varchar("name", 16)
        val checksum = varchar("checksum", 64)
    }

    object BuildMetaTags : IntIdTable("buildmeta_tags") {
        val buildMeta = reference("buildmeta_id", BuildMeta)
        val tag = varchar("tag", 32)
    }
}