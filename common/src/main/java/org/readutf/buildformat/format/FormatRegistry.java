package org.readutf.buildformat.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.requirement.Requirement;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record FormatRegistry(BuildFormatManager buildFormatManager, File directory) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public FormatRegistry(BuildFormatManager buildFormatManager, File directory) {
        this.buildFormatManager = buildFormatManager;
        this.directory = new File(directory, "formats");
        if (!this.directory.exists() && this.directory.mkdirs()) {
            System.out.println("Created formats directory at: " + this.directory.getAbsolutePath());
        }
    }

    public @NotNull @Unmodifiable Map<String, List<Requirement>> readFormats() {
        File[] files = directory.listFiles();
        if (files == null) {
            return Map.of();
        }

        Map<String, List<Requirement>> formats = new HashMap<>();

        for (File file : files) {
            try {
                JsonNode jsonNode = objectMapper.readTree(new FileReader(file));
                List<Requirement> requirements = buildFormatManager.deserializeRequirements(jsonNode);
                String name = file.getName().substring(0, file.getName().lastIndexOf('.'));

                formats.put(name, requirements);
                System.out.printf("Loaded format: %s with %s requirements%n", file.getName(), requirements.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return formats;
    }
}
