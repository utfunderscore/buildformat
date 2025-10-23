package org.readutf.buildformat.postgres;

import org.jooq.*;
import org.jooq.Record;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.readutf.buildformat.Build;
import org.readutf.buildformat.postgres.jooq.Tables;
import org.readutf.buildformat.postgres.jooq.tables.BuildMeta;
import org.readutf.buildformat.postgres.jooq.tables.records.BuildMetaRecord;
import org.readutf.buildformat.postgres.jooq.tables.records.BuildSupportedFormatsRecord;
import org.readutf.buildformat.postgres.jooq.tables.records.BuildVersionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static org.readutf.buildformat.postgres.jooq.Tables.*;

/**
 * Utility class providing queries for managing build metadata and versions in a database.
 */
public class BuildDatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(BuildDatabaseManager.class);

    /**
     * Creates or updates an existing build and auto-increments the current version number.
     * If the build does not exist, it is created. Otherwise, a new version is added.
     * Supported formats are also associated with the new version.
     *
     * @param context     the JOOQ DSL context for database operations
     * @param name        the name of the build
     * @param description the description of the build version
     * @param checksum    the checksum for the build version
     * @param formats     the list of supported format names for this build version
     */
    public static void saveBuild(
            @NonNull DSLContext context,
            @NonNull String name,
            @NonNull String description,
            String checksum,
            @NonNull List<String> formats) {

        context.transaction(config -> {
            DSLContext dsl = config.dsl();

            Result<Record1<Integer>> buildIdResults = dsl.select(Tables.BUILD_META.ID)
                    .from(Tables.BUILD_META)
                    .where(Tables.BUILD_META.BUILD_NAME.equalIgnoreCase(name))
                    .limit(1)
                    .fetch();

            int buildId;

            if (buildIdResults.isEmpty()) {
                log.info("No build exists yet");

                BuildMetaRecord buildMetaRecord = dsl.newRecord(Tables.BUILD_META);
                buildMetaRecord.setBuildName(name);
                buildMetaRecord.setCreatedAt(LocalDateTime.now());

                buildMetaRecord.store();
                buildId = buildMetaRecord.getId();
            } else {
                log.info("Found existing");

                buildId = buildIdResults.getFirst().component1();
            }

            var nextVersion = select(max(BUILD_VERSION.VERSION_NUMBER).add(1))
                    .from(BUILD_VERSION)
                    .where(BUILD_VERSION.BUILD_META_ID.eq(buildId))
                    .limit(1);

            Result<BuildVersionRecord> execute = dsl.insertInto(BUILD_VERSION)
                    .set(BUILD_VERSION.VERSION_NUMBER, coalesce(field(nextVersion), inline(1)))
                    .set(BUILD_VERSION.DESCRIPTION, inline(description))
                    .set(BUILD_VERSION.BUILD_META_ID, buildId)
                    .set(BUILD_VERSION.CHECKSUM, checksum)
                    .returning(BUILD_VERSION.ID)
                    .fetch();

            Integer buildVersionId = execute.getFirst().getId();

            List<BuildSupportedFormatsRecord> records = formats.stream()
                    .map(format -> {
                        BuildSupportedFormatsRecord buildSupportedFormatsRecord =
                                dsl.newRecord(BUILD_SUPPORTED_FORMATS);
                        buildSupportedFormatsRecord.setBuildVersionId(buildVersionId);
                        buildSupportedFormatsRecord.setFormatName(format);
                        return buildSupportedFormatsRecord;
                    })
                    .toList();

            dsl.batchInsert(records).execute();
        });
    }

    /**
     * Retrieves the latest version of a build by name, including its metadata and supported formats.
     *
     * @param context the JOOQ DSL context for database operations
     * @param name    the name of the build to retrieve
     * @return a {@link Build} object representing the latest build version, or null if not found
     */
    public static @Nullable Build getBuild(@NonNull DSLContext context, @NonNull String name) {

        Result<Record> records = context.transactionResult(configuration -> {
            DSLContext dsl = configuration.dsl();

            return dsl.selectFrom(BUILD_META
                            .join(BUILD_VERSION)
                            .on(BUILD_META.ID.eq(BUILD_VERSION.BUILD_META_ID))
                            .leftJoin(BUILD_SUPPORTED_FORMATS)
                            .on(BUILD_VERSION.ID.eq(BUILD_SUPPORTED_FORMATS.BUILD_VERSION_ID)))
                    .where(BUILD_META
                            .BUILD_NAME
                            .eq(inline(name))
                            .and(BUILD_VERSION.VERSION_NUMBER.eq(select(max(BUILD_VERSION.VERSION_NUMBER))
                                    .from(BUILD_VERSION)
                                    .where(BUILD_META.ID.eq(BUILD_VERSION.BUILD_META_ID)))))
                    .fetch();
        });

        if (records.isEmpty()) {
            return null;
        }

        Record first = records.getFirst();
        List<String> formats = new ArrayList<>();

        for (Record record : records) {
            formats.add(record.get(BUILD_SUPPORTED_FORMATS.FORMAT_NAME));
        }

        return new Build(
                first.get(BuildMeta.BUILD_META.BUILD_NAME),
                first.get(BUILD_VERSION.DESCRIPTION),
                first.get(BUILD_META.CREATED_AT),
                first.get(BUILD_VERSION.CHECKSUM),
                first.get(BUILD_VERSION.VERSION_NUMBER),
                formats);
    }

    /**
     * Retrieves a list of the latest builds that support a specific format.
     *
     * @return a list of build names supporting the latest version of a format
     */
    public static @NonNull List<String> getBuildsByFormat(DSLContext context, String format) {

        context.transaction(configuration -> {
            DSLContext dsl = configuration.dsl();

            Table<Record3<String, Integer, Integer>> inner = dsl.select(
                            BUILD_META.BUILD_NAME,
                            BUILD_VERSION.VERSION_NUMBER,
                            rowNumber()
                                    .over(partitionBy(BUILD_META.ID).orderBy(BUILD_VERSION.VERSION_NUMBER.desc()))
                                    .as("rn"))
                    .from(BUILD_META
                            .join(BUILD_VERSION)
                            .on(BUILD_META.ID.eq(BUILD_VERSION.BUILD_META_ID))
                            .join(BUILD_SUPPORTED_FORMATS)
                            .on(BUILD_VERSION.ID.eq(BUILD_SUPPORTED_FORMATS.BUILD_VERSION_ID)))
                    .where(BUILD_SUPPORTED_FORMATS.FORMAT_NAME.eq(inline(format)))
                    .asTable("ranked");

            System.out.println(dsl.select(field(name("ranked", "build_name")), field(name("ranked", "version_number")))
                    .from(inner)
                    .where(field(name("rn")).eq(inline(1)))
                    .fetch());
        });
        return List.of();
    }
}
