package org.readutf.buildformat.plugin.formats;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatManager;
import org.readutf.buildformat.common.format.CompiledBuildFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildFormatStore {

    private @NotNull
    final File directory;
    private @NotNull
    final Logger logger = LoggerFactory.getLogger(BuildFormatStore.class);

    public BuildFormatStore(@NotNull File directory) throws BuildFormatException, IOException {
        this.directory = directory;
        directory.mkdirs();
    }

    public List<String> getFormats() {
        return Arrays.stream(Optional.ofNullable(directory.list()).orElse(new String[0])).map(FilenameUtils::removeExtension).toList();
    }

    public List<CompiledBuildFormat> getFormatChecksums() {
        HashMap<String, byte[]> checksums = new HashMap<>();
        ArrayList<CompiledBuildFormat> formats = new ArrayList<>();

        File[] files = directory.listFiles();
        if (files == null) return new ArrayList<>();

        for (File file : files) {
            try {
                formats.add(BuildFormatManager.load(file.toPath()));
            } catch (IOException e) {
                logger.error("Failed to load build format from file.", e);
            }
        }
        return formats;
    }

}
