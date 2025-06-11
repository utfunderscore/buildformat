package org.readutf.buildstore;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Record;
import org.jooq.Result;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;
import org.readutf.buildformat.common.meta.BuildMeta;
import org.readutf.buildformat.common.meta.BuildMetaStore;
import org.readutf.buildstore.generated.Tables;
import org.readutf.buildstore.generated.tables.records.BuildmetaFormatRecord;
import org.readutf.buildstore.generated.tables.records.BuildmetaRecord;
import org.readutf.buildstore.generated.tables.records.BuildmetaTagsRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresMetaStore implements BuildMetaStore {

    private static final Logger logger = LoggerFactory.getLogger(PostgresMetaStore.class);

    private final @NotNull PostgresDatabaseManager databaseManager;

    public PostgresMetaStore(@NotNull PostgresDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");
    }

    @Override
    public @NotNull BuildMeta create(String name, String description) throws BuildFormatException {
        return databaseManager.runReturning(ctx -> {
            BuildmetaRecord buildmetaRecord = ctx.newRecord(Tables.BUILDMETA);
            buildmetaRecord.setName(name);
            buildmetaRecord.setDescription(description);
            buildmetaRecord.setVersion(0);

            buildmetaRecord.store();

            return new BuildMeta(name, description, 0, Collections.emptyList(), Collections.emptyList());
        });
    }

    @Override
    public @Nullable BuildMeta getByName(String name) throws BuildFormatException {
        return databaseManager.runReturning(ctx -> {
            BuildmetaRecord buildmetaRecord = ctx.fetchOne(Tables.BUILDMETA, Tables.BUILDMETA.NAME.eq(name));
            if (buildmetaRecord == null) {
                return null;
            }

            Result<BuildmetaTagsRecord> tagRecords = ctx.fetch(Tables.BUILDMETA_TAGS, Tables.BUILDMETA_TAGS.BUILDMETA_ID.eq(buildmetaRecord.getId()));

            List<String> tags = tagRecords.map(BuildmetaTagsRecord::getTag);

            Result<BuildmetaFormatRecord> formatRecords = ctx.fetch(Tables.BUILDMETA_FORMAT, Tables.BUILDMETA_FORMAT.BUILDMETA_ID.eq(buildmetaRecord.getId()));

            List<BuildFormatChecksum> checksums = formatRecords.map(record ->
                    new BuildFormatChecksum(record.getName(), Base64.getDecoder().decode(record.getChecksum().getBytes(StandardCharsets.UTF_8)))
            );

            return new BuildMeta(buildmetaRecord.getName(), buildmetaRecord.getDescription(), buildmetaRecord.getVersion(), tags, checksums);
        });
    }

    @Override
    public @Nullable BuildMeta update(String name, List<BuildFormatChecksum> formats) throws BuildFormatException {
        databaseManager.run(context -> {
            BuildmetaRecord buildmetaRecord = context.fetchOne(Tables.BUILDMETA, Tables.BUILDMETA.NAME.eq(name));
            if (buildmetaRecord == null) throw new BuildFormatException("Could not find build meta with name " + name);

            context.deleteFrom(Tables.BUILDMETA_FORMAT).where(Tables.BUILDMETA_FORMAT.BUILDMETA_ID.eq(buildmetaRecord.getId())).execute();

            context.update(Tables.BUILDMETA).set(Tables.BUILDMETA.VERSION, Tables.BUILDMETA.VERSION.add(1))
                    .where(Tables.BUILDMETA.NAME.eq(name))
                    .execute();

            List<BuildmetaFormatRecord> checksums = formats.stream()
                    .map(format -> {
                        String data = Base64.getEncoder().encodeToString(format.checksum());
                        BuildmetaFormatRecord formatRecord = context.newRecord(Tables.BUILDMETA_FORMAT);
                        formatRecord.setName(format.name());
                        formatRecord.setChecksum(data);
                        formatRecord.setBuildmetaId(buildmetaRecord.getId());
                        return formatRecord;
                    }).toList();

            context.batchInsert(checksums).execute();
        });

        return getByName(name);
    }

    @Override
    public @NotNull List<String> getBuilds() throws BuildFormatException {
        return databaseManager.runReturning(context -> {
            Result<BuildmetaRecord> records = context.selectFrom(Tables.BUILDMETA).fetch();
            return records.map(BuildmetaRecord::getName);
        });
    }

    @Override
    public @NotNull Map<String, BuildFormatChecksum> getBuildsByFormat(@NotNull String formatName) throws BuildFormatException {
        return databaseManager.runReturning(context -> {
                    Result<Record> result = context.select()
                            .from(Tables.BUILDMETA.join(Tables.BUILDMETA_FORMAT).on(
                                    Tables.BUILDMETA.ID.eq(Tables.BUILDMETA_FORMAT.BUILDMETA_ID)
                            )).where(Tables.BUILDMETA_FORMAT.NAME.eq(formatName))
                            .fetch();

                    return result.stream().collect(Collectors.toMap(
                            record -> record.get(Tables.BUILDMETA.NAME),
                            record -> new BuildFormatChecksum(
                                    record.get(Tables.BUILDMETA_FORMAT.NAME),
                                    Base64.getDecoder().decode(record.get(Tables.BUILDMETA_FORMAT.CHECKSUM))
                            )
                    ));
                }
        );
    }


}
