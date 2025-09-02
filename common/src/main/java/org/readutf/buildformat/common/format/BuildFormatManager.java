package org.readutf.buildformat.common.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.requirements.Requirement;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.markers.MarkerAdapter;
import org.readutf.buildformat.common.markers.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BuildFormatManager {

    private static final Logger log = LoggerFactory.getLogger(BuildFormatManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @NotNull
    private static final Map<Class<? extends BuildFormat>, List<RequirementData>> generatedRequirements = new HashMap<>();
    @NotNull
    private static final ConcurrentHashMap<Class<?>, MarkerAdapter<?>> adapters = new ConcurrentHashMap<>();

    static {
        adapters.put(Marker.class, marker -> marker);
        adapters.put(Position.class, Marker::getTargetPosition);
        adapters.put(String.class, Marker::toString);
    }

    public static <T> void registerAdapter(Class<? extends T> type, MarkerAdapter<? extends T> adapter) {
        adapters.put(type, adapter);
    }

    @Contract("_, _ -> new")
    public static <T extends BuildFormat> @NotNull CompiledBuildFormat compile(@NotNull String name, @NotNull Class<T> format) throws BuildFormatException {
        return new CompiledBuildFormat(name, getValidators(format));
    }

    public static <T extends BuildFormat> void save(@NotNull Path path, @NotNull String name, @NotNull Class<T> format) throws BuildFormatException, IOException {
        objectMapper.writeValue(path.toFile(), compile(name, format));
    }

    public static CompiledBuildFormat load(@NotNull Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), new TypeReference<>() {
        });
    }

    public static void test(@NotNull List<Marker> markers, @NotNull Path path) throws IOException, BuildFormatException {
        test(markers, load(path));
    }

    public static byte @NotNull [] checksum(@NotNull Path path) throws IOException {
        return checksum(load(path));
    }

    public static <T extends BuildFormat> byte @NotNull [] checksum(@NotNull String name, @NotNull Class<T> format) throws BuildFormatException {
        return checksum(compile(name, format));
    }

    public static byte @NotNull [] checksum(@NotNull CompiledBuildFormat compiledBuildFormat) {
        return ByteBuffer.allocate(4).putInt(compiledBuildFormat.hashCode()).array();
    }

    public static void test(@NotNull List<Marker> markers, @NotNull CompiledBuildFormat compiledBuildFormat) throws BuildFormatException {
        for (RequirementData requirement : compiledBuildFormat.requirements()) {
            log.info("Markers: {}", markers);

            List<Marker> matching = markers.stream().filter(marker -> marker.name().matches(requirement.getRegex())).toList();

            int minimum = requirement.minimumAmount();

            if (matching.size() < minimum) {
                throw new BuildFormatException("Requirement not met: '" + requirement.getExplanation());
            }
        }
    }

    public static <T extends BuildFormat> @NotNull T build(
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
                args[i] = build(markers, (Class<? extends BuildFormat>) parameter.getType());
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
            log.error("Failed to create instance of build format: {}", buildFormat.getName(), e);
            throw new BuildFormatException("Failed to create instance of build format: " + buildFormat.getName());
        }
    }

    /**
     * Generates the requirements for a given build type and build class.
     *
     * @param buildClass The build class.
     * @param <T>        The type of the build class.
     * @return A list of requirement data.
     * @throws BuildFormatException If the build class is not a record or if the requirements are invalid.
     */
    private static <T extends BuildFormat> @NotNull List<RequirementData> getValidators(
            @NotNull Class<T> buildClass
    ) throws BuildFormatException {
        if (!buildClass.isRecord()) throw new BuildFormatException("Build class must be a record");

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

        generatedRequirements.put(buildClass, requirements);

        return requirements;
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
