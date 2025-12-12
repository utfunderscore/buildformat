package org.readutf.buildformat;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.settings.BuildMetadata;
import org.readutf.buildformat.store.BuildDataStore;
import org.readutf.buildformat.store.BuildMetaStore;

import java.util.Map;

public class BuildManager {

    private final BuildMetaStore buildMetaStore;
    private final BuildDataStore buildDataStore;

    public BuildManager(BuildMetaStore buildMetaStore, BuildDataStore buildDataStore) {
        this.buildMetaStore = buildMetaStore;
        this.buildDataStore = buildDataStore;
    }

    @Blocking
    public void saveBuild(@NotNull String name, @NotNull String format, int checksum, @NotNull BuildData buildData, @NotNull BuildMetadata metadata) throws Exception {
        int version = buildMetaStore.saveBuild(name, checksum, format, metadata);
        buildDataStore.save(name, version, buildData);
    }

    @Blocking
    public Build loadBuild(@NotNull String name, int version) throws Exception {
        buildMetaStore.getBuild(name, version);
        BuildData buildData = buildDataStore.get(name, version);
        BuildMeta buildMeta = buildMetaStore.getBuild(name, version);
        return new Build(buildMeta, buildData);
    }

    @Blocking
    public Map<String, Integer> getBuildsByFormat(String format) {
        return buildMetaStore.getBuildsByFormat(format);
    }

}
