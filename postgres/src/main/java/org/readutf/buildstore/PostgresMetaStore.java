package org.readutf.buildstore;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    }

    @Override
    public @NotNull BuildMeta create(String name, String description) throws BuildFormatException {
        return databaseManager.runReturning(ctx -> {
            BuildmetaRecord buildmetaRecord = ctx.newRecord(Tables.BUILDMETA);
            buildmetaRecord.setName(name);
            buildmetaRecord.setDescription(description);

            buildmetaRecord.store();

            return new BuildMeta(name, description, Collections.emptyList(), Collections.emptyList());
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
                    new BuildFormatChecksum(record.getName(), record.getChecksum().getBytes(StandardCharsets.UTF_8))
            );

            return new BuildMeta(buildmetaRecord.getName(), buildmetaRecord.getDescription(), tags, checksums);
        });
    }

    @Override
    public void setFormats(String name, List<BuildFormatChecksum> formats) throws BuildFormatException {
        databaseManager.run(context -> {
            BuildmetaRecord buildmetaRecord = context.fetchOne(Tables.BUILDMETA, Tables.BUILDMETA.NAME.eq(name));
            if (buildmetaRecord == null) throw new BuildFormatException("Could not find build meta with name " + name);

            context.deleteFrom(Tables.BUILDMETA_FORMAT).where(Tables.BUILDMETA_FORMAT.BUILDMETA_ID.eq(buildmetaRecord.getId())).execute();

            List<BuildmetaFormatRecord> checksums = formats.stream()
                    .map(format -> {
                        BuildmetaFormatRecord formatRecord = context.newRecord(Tables.BUILDMETA_FORMAT);
                        formatRecord.setName(format.name());
                        formatRecord.setChecksum(new String(format.checksum(), StandardCharsets.UTF_8));
                        formatRecord.setBuildmetaId(buildmetaRecord.getId());
                        return formatRecord;
                    }).toList();

            context.batchInsert(checksums).execute();
        });
    }

    @Override
    public @NotNull List<String> getBuilds() throws BuildFormatException {
        return databaseManager.runReturning(context -> {
            Result<BuildmetaRecord> records = context.selectFrom(Tables.BUILDMETA).fetch();
            return records.map(BuildmetaRecord::getName);
        });
    }


}
