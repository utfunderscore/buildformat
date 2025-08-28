package org.readutf.buildformat.common.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.nio.file.NotDirectoryException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.markers.Position;
import org.readutf.buildformat.common.format.requirements.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class BuildFormatManagerLegacy {
    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(BuildFormatManagerLegacy.class);
    @NotNull
    private static final Map<Class<? extends BuildFormat>, List<RequirementData>> generatedRequirements = new HashMap<>();
    @NotNull
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final ConcurrentHashMap<Class<?>, MarkerAdapter<?>> adapters = new ConcurrentHashMap<>();

    static {
        adapters.put(Marker.class, marker -> marker);
        adapters.put(Position.class, Marker::getTargetPosition);
        adapters.put(String.class, Marker::toString);
    }

    public static <T> void registerAdapter(Class<? extends T> type, MarkerAdapter<? extends T> adapter) {
        adapters.put(type, adapter);
    }

    public static List<RequirementData> load(File file) throws IOException {
        return objectMapper.readValue(file, new TypeReference<>() {
        });
    }

    public static void save(File directory, String name, List<RequirementData> requirementData) throws IOException {
        if (!directory.isDirectory()) {
            throw new NotDirectoryException(directory.getAbsolutePath());
        }

        try (FileOutputStream fos = new FileOutputStream(new File(directory, name + ".json"))) {
            objectMapper.writeValue(fos, requirementData);
        }
    }

    public static void testRequirements(List<Marker> markers, List<@NotNull RequirementData> requirementData) throws BuildFormatException {
        for (RequirementData requirement : requirementData) {
            logger.info("Markers: {}", markers);

            List<Marker> matching = markers.stream().filter(marker -> marker.name().matches(requirement.getRegex())).toList();

            int minimum = requirement.minimumAmount();

            if (matching.size() < minimum) {
                throw new BuildFormatException("Build did not meet requirement: " + requirement.getExplanation());
            }
        }
    }

    public static byte @NotNull [] generateChecksum(@NotNull List<@NotNull RequirementData> requirementData) {
        return ByteBuffer.allocate(4).putInt(requirementData.hashCode()).array();
    }

    /**
     * Retrieves the requirement annotation from a parameter.
     *
     * @param parameter The parameter to check.
     * @return The requirement annotation, or null if not found.
     */
    @Contract(pure = true)
    private static @Nullable Requirement getRequirement(@NotNull Parameter parameter) {

        Annotation[] annotations = parameter.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Requirement) {
                return (Requirement) annotation;
            }
        }
        return null;
    }

}
