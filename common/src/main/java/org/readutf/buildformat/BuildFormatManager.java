package org.readutf.buildformat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.RegisteredRequirement;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.factory.RequirementFactory;
import org.readutf.buildformat.requirement.factory.impl.*;
import org.readutf.buildformat.requirement.types.list.PositionListRequirement;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.utils.ClassUtils;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;

public class BuildFormatManager {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final BuildFormatManager INSTANCE = new BuildFormatManager();

    private final List<RegisteredRequirement> factories;
    private final Map<Class<? extends Requirement>, RequirementFactory> serializers = new HashMap<>();

    public BuildFormatManager() {
        this.factories = new ArrayList<>();

        this.registerFactory(new TypeReference<List<Position>>() {}, new PositionListRequirementFactory());
        this.registerFactory(new TypeReference<Position>() {}, new PositionRequirementFactory());
        this.registerFactory(new TypeReference<Integer>() {}, new IntegerRequirementFactory());
        this.registerFactory(new TypeReference<Double>() {}, new DoubleRequirementFactory());
        this.registerFactory(new TypeReference<Long>() {}, new LongRequirementFactory());
        this.registerFactory(new TypeReference<String>() {}, new StringRequirementFactory());
    }

    public void registerFactory(@NotNull TypeReference<?> reference, @NotNull RequirementFactory factory) {
        this.factories.add(new RegisteredRequirement(reference, factory));
        this.serializers.put(factory.getRequirementType(), factory);
    }

    public List<Requirement> generateRequirements(@NotNull Class<?> clazz) throws Exception {
        if (!clazz.isRecord()) {
            throw new Exception("Class must be a record");
        }
        List<Requirement> requirements = new ArrayList<>();

        for (RecordComponent recordComponent : clazz.getRecordComponents()) {
            Class<?> parameterType = recordComponent.getType();

            if (parameterType.isPrimitive()) {
                parameterType = ClassUtils.getWrapperClass(parameterType);
            }

            RequirementFactory factory = null;
            for (RegisteredRequirement registeredRequirement : getFactories()) {
                Type type = registeredRequirement.reference().getType();
                if (type instanceof ParameterizedType parameterizedType
                        && recordComponent.getGenericType() instanceof ParameterizedType parameterizedType2) {
                    if (ClassUtils.equals(parameterizedType, parameterizedType2)) {
                        factory = registeredRequirement.factory();
                        break;
                    }
                }
                if (type instanceof Class<?> cls && parameterType.isAssignableFrom(cls)) {
                    factory = registeredRequirement.factory();
                    break;
                }
            }
            if (factory == null) {
                throw new Exception("No factory found for type: " + parameterType.getName());
            }

            ParameterizedType parameterizedType = null;
            if(recordComponent.getGenericType() instanceof ParameterizedType p) {
                parameterizedType = p;
            }

            Requirement requirement = factory.createRequirement(
                    recordComponent.getName(),
                    recordComponent.getType(),
                    recordComponent.getAnnotations(),
                    parameterizedType);
            requirements.add(requirement);

        }
        return requirements;
    }

    @SuppressWarnings("unchecked")
    public <T> T construct(@NotNull Class<T> clazz, @NotNull Map<String, Object> data) throws Exception {
        if (!clazz.isRecord()) {
            throw new Exception("Class must be a record");
        }

        List<Object> constructorArgs = new ArrayList<>();
        for (RecordComponent recordComponent : clazz.getRecordComponents()) {
            Object value = data.get(recordComponent.getName());
            constructorArgs.add(objectMapper.convertValue(value, recordComponent.getType()));
        }

        // This assumes that the record has a single canonical constructor
        return (T) clazz.getDeclaredConstructors()[0].newInstance(constructorArgs.toArray());

    }

    public int checksum(@NotNull List<Requirement> requirements) {
        return requirements.hashCode();
    }

    public JsonNode serializeRequirements(@NotNull List<Requirement> requirements) throws Exception {

        List<Map<String, Object>> data = new ArrayList<>();

        for (Requirement requirement : requirements) {
            RequirementFactory factory = this.serializers.get(requirement.getClass());
            if (factory == null) {
                throw new Exception("No serializer found for requirement type: "
                        + requirement.getClass().getName());
            }

            Map<String, Object> serializerData = new HashMap<>();
            serializerData.put("name", factory.getRequirementType());
            serializerData.put("data", factory.serialize(objectMapper, requirement));
            data.add(serializerData);
        }

        return objectMapper.valueToTree(data);
    }

    public void serializeRequirements(@NotNull File file,  @NotNull List<Requirement> requirements) throws Exception {
        JsonNode jsonNode = serializeRequirements(requirements);
        objectMapper.writeValue(file, jsonNode);
    }

    public List<Requirement> deserializeRequirements(@NotNull JsonNode jsonNode) throws Exception {

        List<Requirement> requirements = new ArrayList<>();
        for (JsonNode node : jsonNode) {
            String name = node.get("name").asText();
            JsonNode data = node.get("data");

            RequirementFactory factory = this.serializers.get(Class.forName(name).asSubclass(Requirement.class));
            if (factory == null) {
                throw new Exception("No deserializer found for requirement type: " + name);
            }

            System.out.println(name);
            Requirement requirement = factory.deserialize(objectMapper, data);
            requirements.add(requirement);
        }
        return requirements;
    }

    public List<RegisteredRequirement> getFactories() {
        return factories;
    }

    public Map<Class<? extends Requirement>, RequirementFactory> getSerializers() {
        return serializers;
    }

    public static BuildFormatManager getInstance() {
        return INSTANCE;
    }

}
