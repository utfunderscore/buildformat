package org.readutf.buildformat.sql

import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.readutf.buildformat.common.exception.BuildFormatException
import org.readutf.buildformat.common.format.BuildFormatChecksum
import org.readutf.buildformat.common.meta.BuildMeta
import org.readutf.buildformat.common.meta.BuildMetaStore
import java.util.Base64

open class ExposedMetaStore private constructor(
    val database: Database
) : BuildMetaStore {

    constructor(datasource: HikariDataSource) : this(
        Database.connect(datasource = datasource)
    )

    init {
        transaction(database) {
            SchemaUtils.create(
                Tables.BuildMeta,
                Tables.BuildMetaFormat,
                Tables.BuildMetaTags
            )
        }
    }

    override fun create(
        name: String,
        description: String
    ): BuildMeta {
        try {
            transaction(database) {
                Tables.BuildMeta.insert {
                    it[this.name] = name
                    it[this.description] = description
                }
            }
        } catch (e: ExposedSQLException) {
            throw BuildFormatException(
                "Failed to create build meta with name $name. It may already exist.",
                e
            )
        }

        return BuildMeta(
            name,
            description,
            emptyList(),
            emptyList()
        )
    }

    override fun getByName(name: String): BuildMeta? {
        return transaction(database) {
            val buildMetaRow = Tables.BuildMeta
                .selectAll()
                .where { Tables.BuildMeta.name eq name }
                .singleOrNull()

            if (buildMetaRow == null) {
                null
            } else {
                val id: EntityID<Int> = buildMetaRow[Tables.BuildMeta.id]

                val tags = Tables.BuildMetaTags
                    .selectAll()
                    .where { Tables.BuildMetaTags.buildMeta eq id }
                    .map { it[Tables.BuildMetaTags.tag] }

                val checksums = Tables.BuildMetaFormat
                    .selectAll()
                    .where { Tables.BuildMetaFormat.buildMeta eq id }
                    .map {
                        BuildFormatChecksum(
                            it[Tables.BuildMetaFormat.name],
                            Base64.getDecoder()
                                .decode(it[Tables.BuildMetaFormat.checksum].toByteArray(Charsets.UTF_8))
                        )
                    }

                BuildMeta(
                    buildMetaRow[Tables.BuildMeta.name],
                    buildMetaRow[Tables.BuildMeta.description],
                    tags,
                    checksums
                )
            }
        }
    }

    override fun update(
        name: String,
        formats: List<BuildFormatChecksum?>
    ) {
        return transaction(database) {
            val buildMetaRow = Tables.BuildMeta
                .selectAll()
                .where { Tables.BuildMeta.name eq name }
                .singleOrNull() ?: throw BuildFormatException("Could not find build meta with name $name")

            val id = buildMetaRow[Tables.BuildMeta.id]
            Tables.BuildMetaFormat.deleteWhere { Tables.BuildMetaFormat.buildMeta eq id }

            formats.filterNotNull().forEach { format ->
                Tables.BuildMetaFormat.insert {
                    it[this.name] = format.name
                    it[this.checksum] = Base64.getEncoder().encodeToString(format.checksum)
                    it[this.buildMeta] = id
                }
            }

        }
    }

    override fun getBuilds(): List<String?> {
        return transaction(database) {
            Tables.BuildMeta
                .selectAll()
                .map { it[Tables.BuildMeta.name] }
        }
    }

    override fun getBuildsByFormat(formatName: String): Map<String?, BuildFormatChecksum?> {
        return transaction(database) {
            Tables.BuildMetaFormat
                .selectAll()
                .where { Tables.BuildMetaFormat.name eq formatName }
                .associate {
                    val buildMeta = Tables.BuildMeta.selectAll()
                        .where { Tables.BuildMeta.id eq it[Tables.BuildMetaFormat.buildMeta] }
                        .singleOrNull()

                    val checksum = it[Tables.BuildMetaFormat.checksum]
                    val decodedChecksum = Base64.getDecoder().decode(checksum.toByteArray(Charsets.UTF_8))

                    buildMeta?.get(Tables.BuildMeta.name) to BuildFormatChecksum(
                        it[Tables.BuildMetaFormat.name],
                        decodedChecksum
                    )
                }
        }
    }

}