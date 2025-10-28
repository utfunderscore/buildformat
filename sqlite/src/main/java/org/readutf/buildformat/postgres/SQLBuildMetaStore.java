package org.readutf.buildformat.postgres;

import org.jetbrains.annotations.Unmodifiable;
import org.jooq.*;
import org.jooq.Record;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.BuildMeta;
import org.readutf.buildformat.postgres.jooq.Tables;
import org.readutf.buildformat.postgres.jooq.tables.records.BuildMetaRecord;
import org.readutf.buildformat.postgres.jooq.tables.records.BuildSupportedFormatsRecord;
import org.readutf.buildformat.postgres.jooq.tables.records.BuildVersionRecord;
import org.readutf.buildformat.store.BuildMetaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.readutf.buildformat.postgres.jooq.Tables.*;

/**
 * Utility class providing queries for managing build metadata and versions in a database.
 */
public class SQLBuildMetaStore implements BuildMetaStore {

    private static final Logger log = LoggerFactory.getLogger(SQLBuildMetaStore.class);

    private final DSLContext context;

    public SQLBuildMetaStore(DSLContext context) {
        this.context = context;
    }


    public int saveBuild(
             @NotNull String name, String checksum, @NotNull String format) {

        return context.transactionResult(config -> {
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
                    .set(BUILD_VERSION.BUILD_META_ID, buildId)
                    .set(BUILD_VERSION.CHECKSUM, checksum)
                    .returning(BUILD_VERSION.ID, BUILD_VERSION.VERSION_NUMBER)
                    .fetch();

            BuildVersionRecord first = execute.getFirst();
            Integer buildVersionId = first.getId();
            Integer version = first.getVersionNumber();

            BuildSupportedFormatsRecord buildSupportedFormatsRecord = dsl.newRecord(BUILD_SUPPORTED_FORMATS);
            buildSupportedFormatsRecord.setBuildVersionId(buildVersionId);
            buildSupportedFormatsRecord.setFormatName(format);
            buildSupportedFormatsRecord.store();

            return version;
        });
    }

    public @Nullable BuildMeta getBuild( @NotNull String name) {

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

        return firstRecordToBuildMeta(records);
    }

    @Override
    public @Nullable BuildMeta getBuild(@NotNull String name, int version) {

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
                            .and(BUILD_VERSION.VERSION_NUMBER.eq(inline(version))))
                    .fetch();
        });

        return firstRecordToBuildMeta(records);
    }

    @Nullable
    private BuildMeta firstRecordToBuildMeta(@NotNull Result<Record> records) {
        if (records.isEmpty()) {
            return null;
        }

        Record first = records.getFirst();
        String format = first.get(BUILD_SUPPORTED_FORMATS.FORMAT_NAME);

        return new BuildMeta(
                first.get(org.readutf.buildformat.postgres.jooq.tables.BuildMeta.BUILD_META.BUILD_NAME),
                first.get(BUILD_META.CREATED_AT),
                first.get(BUILD_VERSION.CHECKSUM),
                first.get(BUILD_VERSION.VERSION_NUMBER),
                format);
    }

    /**
     * Retrieves a list of the latest builds that support a specific format.
     *
     * @return a map where the key is the build name and the value is the latest version number
     */
    public @NotNull @Unmodifiable Map<String, Integer> getBuildsByFormat(
             @NotNull String format) {

        return context.transactionResult(configuration -> {
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

            return dsl.select(field(name("ranked", "build_name")), field(name("ranked", "version_number")))
                    .from(inner)
                    .where(field(name("rn")).eq(inline(1)))
                    .fetch().stream().collect(Collectors.toMap(record -> record.get(0, String.class), record -> record.get(1, Integer.class)));
        });
    }
}
