package org.readutf.buildformat.common.meta;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;

public interface BuildMetaStore {

    @NotNull BuildMeta create(String name, String description) throws BuildFormatException;

    @Nullable BuildMeta getByName(String name) throws BuildFormatException;

    void setFormats(String name, List<BuildFormatChecksum> formats) throws BuildFormatException;

    @NotNull List<String> getBuilds() throws BuildFormatException;

    @NotNull List<String> getBuildsByFormat(@NotNull String formatName) throws BuildFormatException;


}
