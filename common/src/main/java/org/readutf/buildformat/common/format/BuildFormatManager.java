package org.readutf.buildformat.common.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.requirements.Requirement;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.markers.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.*;

public class BuildFormatManager {

    @NotNull
    private static final Logger logger = LoggerFactory.getLogger(BuildFormatManager.class);

    @NotNull
    private final ObjectMapper objectMapper;

    @NotNull
    private final Map<Class<? extends BuildFormat>, List<RequirementData>> cachedRequirements;

    @NotNull
    private final Map<Class<?>, MarkerAdapter<?>> adapters;

    private BuildFormatManager(@NotNull ObjectMapper objectMapper, @NotNull Map<Class<?>, MarkerAdapter<?>> adapters) {
        this.objectMapper = objectMapper;
        this.adapters = adapters;
        this.cachedRequirements = new HashMap<>();

        //Register default adapters
        this.adapters.put(Marker.class, marker -> marker);
        this.adapters.put(Position.class, Marker::getTargetPosition);
        this.adapters.put(String.class, Marker::toString);
    }

    @SuppressWarnings("unchecked")
    public <T extends BuildFormat> @NotNull T read(
            @NotNull List<Marker> markers,
            @NotNull Class<T> buildFormat
    ) throws BuildFormatException {
        List<RequirementData> requirementData = cachedRequirements.get(buildFormat);
        if (requirementData == null) {
            requirementData = getValidators(buildFormat);
            cachedRequirements.put(BuildFormat.class, requirementData);
        }

        Constructor<?> constructor = buildFormat.getDeclaredConstructors()[0];

        Object[] args = new Object[constructor.getParameterCount()];

        for (int i = 0; i < constructor.getParameters().length; i++) {
            Parameter parameter = constructor.getParameters()[i];
            RequirementData requirement = requirementData.get(i);

            if (BuildFormat.class.isAssignableFrom(parameter.getType())) {
                args[i] = read(markers, (Class<? extends BuildFormat>) parameter.getType());
                continue;
            }

            Class<?> parameterType = parameter.getType();


            List<Marker> matching = markers.stream().filter(marker -> marker.name().matches(requirement.getRegex())).toList();

            if (matching.size() < Math.max(1, requirement.minimumAmount())) {
                throw new BuildFormatException("Not enough markers found for parameter: " + parameter.getName());
            }

            if (parameterType == List.class || parameterType == Collection.class) {
                ParameterizedType listType = (ParameterizedType) parameter.getParameterizedType();
                Type actualType = listType.getActualTypeArguments()[0];
                MarkerAdapter<?> markerAdapter = adapters.get(((Class<?>) actualType));

                if (markerAdapter != null) {
                    args[i] = matching.stream().map(markerAdapter::adapt).toList();
                } else {
                    throw new BuildFormatException("Invalid list parameter type: " + actualType.getTypeName());
                }


            } else if (adapters.containsKey(parameterType)) {
                MarkerAdapter<?> markerAdapter = adapters.get(parameterType);
                args[i] = markerAdapter.adapt(matching.getFirst());
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

    public <T extends BuildFormat> void save(@NotNull Path path, @NotNull Class<T> buildFormat) throws BuildFormatException, IOException {
        save(path, getValidators(buildFormat));
    }

    public <T extends BuildFormat> int getChecksum(@NotNull Class<T> buildFormatClass) throws BuildFormatException {
        List<RequirementData> validators = getValidators(buildFormatClass);
        return validators.hashCode();
    }

    /**
     * Generates the requirements for a given build type and build class.
     *
     * @param buildClass The build class.
     * @param <T>        The type of the build class.
     * @return A list of requirement data.
     * @throws BuildFormatException If the build class is not a record or if the requirements are invalid.
     */
    private <T extends BuildFormat> @NotNull List<RequirementData> getValidators(
            @NotNull Class<T> buildClass
    ) throws BuildFormatException {
        if (!buildClass.isRecord()) throw new BuildFormatException("Build class must be a record");
        List<RequirementData> requirementData = cachedRequirements.get(buildClass);
        if (requirementData != null) {
            return requirementData;
        }

        Constructor<?> constructor = buildClass.getDeclaredConstructors()[0];

        List<RequirementData> requirements = new ArrayList<>();

        for (Parameter parameter : constructor.getParameters()) {
            Requirement requirement = getRequirement(parameter);
            Class<?> type = parameter.getType();

            if (!adapters.containsKey(type) && !Collection.class.isAssignableFrom(type)) {
                if (!BuildFormat.class.isAssignableFrom(type)) {
                    throw new BuildFormatException("Invalid build requirement type: " + type);
                }
                Class<? extends BuildFormat> subclass = type.asSubclass(BuildFormat.class);
                requirements.addAll(getValidators(subclass));
                continue;
            }

            if (requirement == null) {
                throw new BuildFormatException("Requirement annotation not found on parameter: " + parameter.getName());
            }

            int minimum = requirement.minimum();
            if (minimum < 1) {
                throw new BuildFormatException("Minimum value must be greater than or equal to 1");
            }

            requirements.add(new RequirementData(
                    requirement.name().isEmpty() ? null : requirement.name(),
                    requirement.startsWith().isEmpty() ? null : requirement.startsWith(),
                    requirement.endsWith().isEmpty() ? null : requirement.endsWith(),
                    minimum
            ));
        }

        cachedRequirements.put(buildClass, requirements);

        return requirements;
    }

    private void save(@NotNull Path path, @NotNull List<RequirementData> requirementData) throws IOException {
        File directory = path.toFile().getParentFile();
        if (!directory.exists() && directory.mkdirs()) {
            logger.info("Generated build format directory {}", directory.getAbsolutePath());
        }

        if (!directory.isDirectory()) {
            throw new NotDirectoryException(directory.getAbsolutePath());
        }

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            objectMapper.writeValue(fos, requirementData);
        }
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


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<Class<?>, MarkerAdapter<?>> adapters;
        private @NotNull ObjectMapper objectMapper;

        private Builder() {
            this.adapters = new HashMap<>();
            this.objectMapper = new ObjectMapper();
        }

        public <T> Builder registerAdapter(Class<? extends T> type, MarkerAdapter<? extends T> adapter) {
            adapters.put(type, adapter);
            return this;
        }

        public Builder setObjectMapper() {
            this.objectMapper = new ObjectMapper();
            return this;
        }

        public BuildFormatManager build() {
            return new BuildFormatManager(objectMapper, adapters);
        }

    }

}
