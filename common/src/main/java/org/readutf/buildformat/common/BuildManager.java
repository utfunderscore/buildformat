package org.readutf.buildformat.common;

import java.util.Map;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;
import org.readutf.buildformat.common.meta.BuildMeta;
import org.readutf.buildformat.common.meta.BuildMetaStore;
import org.readutf.buildformat.common.schematic.BuildData;
import org.readutf.buildformat.common.schematic.BuildSchematicStore;

public class BuildManager {

    private @NotNull final BuildMetaStore buildMetaStore;
    private @NotNull final BuildSchematicStore schematicStore;

    public BuildManager(@NotNull BuildMetaStore buildMetaStore, @NotNull BuildSchematicStore schematicStore) {
        this.buildMetaStore = buildMetaStore;
        this.schematicStore = schematicStore;
    }

    @Blocking
    @Nullable
    public Build getBuild(String name) throws BuildFormatException {
        BuildMeta meta = buildMetaStore.getByName(name);
        if (meta == null) return null;

        BuildData buildData = schematicStore.load(name);
        if (buildData == null) return null;
        return new Build(meta, buildData);
    }

    @Blocking
    @NotNull
    public Map<String, BuildFormatChecksum> getBuildsByFormat(@NotNull String formatName) throws BuildFormatException {
        return buildMetaStore.getBuildsByFormat(formatName);
    }

}
