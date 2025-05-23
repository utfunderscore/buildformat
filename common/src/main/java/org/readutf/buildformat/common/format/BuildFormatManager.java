package org.readutf.buildformat.common.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.markers.Position;
import org.readutf.buildformat.common.format.requirements.Requirement;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildFormatManager {

    private @NotNull static final Logger logger = LoggerFactory.getLogger(BuildFormatManager.class);
    private @NotNull static final Map<Class<? extends BuildFormat>, List<RequirementData>> generatedRequirements = new HashMap<>();
    private @NotNull static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates the requirements for a given build type and build class.
     *
     * @param buildClass The build class.
     * @param <T>        The type of the build class.
     * @return A list of requirement data.
     * @throws BuildFormatException If the build class is not a record or if the requirements are invalid.
     */
    public static <T extends BuildFormat> @NotNull List<RequirementData> getValidators(
            @NotNull Class<T> buildClass
    ) throws BuildFormatException {
        if (!buildClass.isRecord()) throw new BuildFormatException("Build class must be a record");

        Constructor<?> constructor = buildClass.getDeclaredConstructors()[0];

        List<RequirementData> requirements = new ArrayList<>();

        for (Parameter parameter : constructor.getParameters()) {
            Requirement requirement = getRequirement(parameter);
            if (BuildFormat.class.isAssignableFrom(parameter.getType())) {
                requirements.add(null);
                continue;
            }

            if (requirement == null) {
                throw new BuildFormatException("Requirement annotation not found on parameter: " + parameter.getName());
            }

            String regex = getRegex(requirement);
            int minimum = requirement.minimum();
            if (minimum < 1) {
                throw new BuildFormatException("Minimum value must be greater than or equal to 1");
            }

            requirements.add(new RequirementData(regex, minimum));
        }

        generatedRequirements.put(buildClass, requirements);

        return requirements;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BuildFormat> @NotNull T constructBuildFormat(
            @NotNull List<Marker> markers,
            @NotNull Class<T> buildFormat
    ) throws BuildFormatException {
        List<RequirementData> requirementData = generatedRequirements.get(buildFormat);
        if (requirementData == null) {
            requirementData = getValidators(buildFormat);
        }

        Constructor<?> constructor = buildFormat.getDeclaredConstructors()[0];

        Object[] args = new Object[constructor.getParameterCount()];

        for (int i = 0; i < constructor.getParameters().length; i++) {
            Parameter parameter = constructor.getParameters()[i];
            RequirementData requirement = requirementData.get(i);

            if (BuildFormat.class.isAssignableFrom(parameter.getType())) {
                args[i] = constructBuildFormat(markers, (Class<? extends BuildFormat>) parameter.getType());
                continue;
            }

            Class<?> parameterType = parameter.getType();

            boolean isMarker = parameterType == Marker.class;
            boolean isPosition = parameterType == Position.class;

            List<Marker> matching = markers.stream().filter(marker -> marker.name().matches(requirement.regex())).toList();

            int minumum = requirement.minimum();
            if (isMarker || isPosition) minumum = Math.max(1, minumum);

            if (matching.size() < minumum) {
                throw new BuildFormatException("Not enough markers found for parameter: " + parameter.getName());
            }

            if (parameterType == List.class) {
                ParameterizedType listType = (ParameterizedType) parameter.getParameterizedType();
                Type actualType = listType.getActualTypeArguments()[0];
                if(actualType == Marker.class) {
                    args[i] = new ArrayList<>(matching);
                } else if (actualType == Position.class) {
                    args[i] = matching.stream().map(Marker::position).toList();
                } else {
                    throw new BuildFormatException("Invalid list parameter type: " + actualType.getTypeName());
                }


            } else if (isMarker) {
                args[i] = matching.getFirst();
            } else if (isPosition) {
                args[i] = matching.getFirst().position();
            } else {
                throw new BuildFormatException("Invalid parameter type: " + parameterType.getName());
            }

        }

        try {
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            logger.error("Failed to create instance of build format: {}", buildFormat.getName(), e);
            throw new BuildFormatException("Failed to create instance of build format: " + buildFormat.getName());
        }

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

            List<Marker> matching = markers.stream().filter(marker -> marker.name().matches(requirement.regex())).toList();

            int minumum = requirement.minimum();

            if (matching.size() < minumum) {
                throw new BuildFormatException("Not enough markers found for requirement: " + requirement.regex());
            }
        }
    }

    public static byte[] generateChecksum(List<@NotNull RequirementData> requirementData) {
        return ByteBuffer.allocate(4).putInt(requirementData.hashCode()).array();
    }

    /**
     * Converts a requirement annotation to a regex string.
     *
     * @param requirement The requirement annotation.
     * @return The regex string.
     */
    private static @NotNull String getRegex(@NotNull Requirement requirement) throws BuildFormatException {
        @NotNull String regex;
        if (!requirement.name().isEmpty()) {
            return "\\b" + requirement.name() + "\\b";
        }
        if (!requirement.endsWith().isEmpty()) {
            regex = "^" + requirement.startsWith() + ".*" + requirement.endsWith() + "$";
        } else if (!requirement.startsWith().isEmpty()) {
            regex = "^" + requirement.startsWith() + ".*$";
        } else if (!requirement.regex().isEmpty()) {
            regex = requirement.regex();
        } else {
            throw new BuildFormatException("Requirement annotation must have at least one of startsWith, endsWith or regex");
        }
        return regex;
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
