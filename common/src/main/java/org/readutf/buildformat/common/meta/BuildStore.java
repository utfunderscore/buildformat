package org.readutf.buildformat.common.meta;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;

public interface BuildStore {

    @NotNull BuildMeta create(String name, String description) throws BuildFormatException;

    @Nullable BuildMeta getByName(String name) throws BuildFormatException;

    BuildMeta update(String name, List<BuildFormatChecksum> formats) throws BuildFormatException;

    @NotNull List<String> getBuilds() throws BuildFormatException;

    @NotNull Map<String, BuildFormatChecksum> getBuildsByFormat(@NotNull String formatName) throws BuildFormatException;


}
