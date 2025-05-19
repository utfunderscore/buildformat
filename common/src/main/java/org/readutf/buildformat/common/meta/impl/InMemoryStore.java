package org.readutf.buildformat.common.meta.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;
import org.readutf.buildformat.common.meta.BuildMeta;
import org.readutf.buildformat.common.meta.BuildStore;

public class InMemoryStore implements BuildStore {

    private final Map<String, BuildMeta> buildMetas = new HashMap<>();

    @Override
    public @NotNull BuildMeta create(String name, String description) throws BuildFormatException {
        if(buildMetas.containsKey(name)) {
            throw new BuildFormatException("Build with name " + name + " already exists");
        }
        BuildMeta buildMeta = new BuildMeta(name, description, 1, List.of(), List.of());
        buildMetas.put(name, buildMeta);
        return buildMeta;
    }

    @Override
    public @Nullable BuildMeta getByName(String name) {
        return buildMetas.get(name);
    }

    @Override
    public BuildMeta update(String name, List<BuildFormatChecksum> formats) throws BuildFormatException {
        BuildMeta buildMeta = buildMetas.get(name);
        if (buildMeta == null) {
            throw new BuildFormatException("Build with name " + name + " does not exist");
        }
        BuildMeta updated = new BuildMeta(
                buildMeta.name(),
                buildMeta.description(),
                1,
                buildMeta.tags(),
                formats
        );
        buildMetas.put(name, updated);
        return buildMeta;
    }

    @Override
    public @NotNull List<String> getBuilds() throws BuildFormatException {
        return List.of();
    }

    @Override
    public @NotNull Map<String, BuildFormatChecksum> getBuildsByFormat(@NotNull String formatName) throws BuildFormatException {
        return new HashMap<>();
    }
}
