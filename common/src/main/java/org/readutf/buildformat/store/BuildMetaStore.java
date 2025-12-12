package org.readutf.buildformat.store;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.BuildMeta;

import java.util.List;
import java.util.Map;

public interface BuildMetaStore {

    /**
     * Saves a build with the given name, checksum, and format.
     * If the build already exists, it updates the existing build.
     *
     * @param name     the name of the build
     * @param checksum the checksum of the build
     * @param format   the format of the build
     * @return the version number of the saved build
     */
    int saveBuild(@NotNull String name, String checksum, String format, Map<String, ?> settings) throws Exception;

    @Nullable
    BuildMeta getBuild(@NotNull String name) throws Exception;

    @Nullable
    BuildMeta getBuild(@NotNull String name, int version) throws Exception;

    @NotNull Map<String, Integer> getBuildsByFormat(String format);

}
