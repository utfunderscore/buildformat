package org.readutf.buildformat.plugin.formats;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatManager;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildFormatCache {

    private final File directory;
    private final Logger logger = LoggerFactory.getLogger(BuildFormatCache.class);

    public BuildFormatCache(@NotNull File directory) throws BuildFormatException, IOException {
        this.directory = directory;
        directory.mkdirs();
    }

    public List<String> getFormats() {
        return Arrays.stream(Optional.ofNullable(directory.list()).orElse(new String[0])).map(FilenameUtils::removeExtension).toList();
    }

    public @NotNull List<RequirementData> getRequirements(String name) throws BuildFormatException {

        if (!directory.exists() && directory.mkdirs()) {
            logger.info("Creating directory {}", directory.getAbsolutePath());
        }

        if (!directory.isDirectory()) {
            throw new BuildFormatException("Not a directory: " + directory.getAbsolutePath());
        }

        File file = new File(directory, name + ".json");
        if (!file.exists()) {
            logger.info("File {} does not exist", file.getAbsolutePath());
            throw new BuildFormatException("File does not exist: " + file.getName());
        }
        try {
            return BuildFormatManager.load(file);
        } catch (IOException e) {
            logger.error("Failed to read file {}", file.getAbsolutePath(), e);
            throw new BuildFormatException("Failed to read file: " + file.getAbsolutePath(), e);
        }
    }

}
