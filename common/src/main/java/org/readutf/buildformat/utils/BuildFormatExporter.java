package org.readutf.buildformat.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.requirement.Requirement;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

public class BuildFormatExporter {

    public static void export(Class<?> clazz, @NotNull Path path)  {
        try {
            List<Requirement> requirements = BuildFormatManager.getInstance().generateRequirements(clazz);
            new ObjectMapper().writeValue(new FileWriter(path.toFile()), BuildFormatManager.getInstance().serializeRequirements(requirements));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
