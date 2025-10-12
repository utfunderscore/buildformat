package org.readutf.buildformat.common.meta;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;

public interface BuildMetaStore {

    @NotNull BuildMeta create(@NotNull String name, @NotNull String description) throws BuildFormatException;

    @Nullable BuildMeta getByName(@NotNull String name) throws BuildFormatException;

    void update(@NotNull String name, @NotNull List<BuildFormatChecksum> checksums) throws BuildFormatException;

    @NotNull List<String> getBuilds() throws BuildFormatException;

    @NotNull Map<String, BuildFormatChecksum> getBuildsByFormat(@NotNull String formatName) throws BuildFormatException;


}
