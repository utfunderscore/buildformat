package org.readutf.buildformat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.factory.RequirementFactory;
import org.readutf.buildformat.requirement.factory.impl.SimpleRequirementFactory;
import org.readutf.buildformat.requirement.factory.impl.ListRequirementFactory;
import org.readutf.buildformat.requirement.factory.impl.NumberRequirementFactory;
import org.readutf.buildformat.types.Position;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.util.*;

public class BuildFormatManager {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Class<?>, RequirementFactory> factories;
    private final Map<Class<? extends Requirement>, RequirementFactory> serializers = new HashMap<>();

    public BuildFormatManager() {
        this.factories = new HashMap<>();

        // List
        this.registerFactory(List.class, new ListRequirementFactory(this));

        // Position
        this.registerFactory(Position.class, new SimpleRequirementFactory());

        // Numbers
        this.registerFactory(Integer.class, new NumberRequirementFactory<>(Integer.MIN_VALUE, Integer.MAX_VALUE));
        this.registerFactory(int.class, new NumberRequirementFactory<>(Integer.MIN_VALUE, Integer.MAX_VALUE));
        this.registerFactory(Double.class, new NumberRequirementFactory<>(Double.MIN_VALUE, Double.MAX_VALUE));
        this.registerFactory(double.class, new NumberRequirementFactory<>(Double.MIN_VALUE, Double.MAX_VALUE));
        this.registerFactory(Float.class, new NumberRequirementFactory<>(Long.MIN_VALUE, Long.MAX_VALUE));
        this.registerFactory(float.class, new NumberRequirementFactory<>(Long.MIN_VALUE, Long.MAX_VALUE));
        this.registerFactory(Long.class, new NumberRequirementFactory<>(Long.MIN_VALUE, Long.MAX_VALUE));
        this.registerFactory(long.class, new NumberRequirementFactory<>(Long.MIN_VALUE, Long.MAX_VALUE));

        // Text
        this.registerFactory(String.class, new SimpleRequirementFactory());
    }

    public void registerFactory(@NotNull Class<?> type, @NotNull RequirementFactory factory) {
        this.factories.put(type, factory);
        this.serializers.put(factory.getRequirementType(), factory);
    }

    public List<Requirement> generateRequirements(@NotNull Class<?> clazz) throws Exception {
        if (!clazz.isRecord()) {
            throw new Exception("Class must be a record");
        }
        List<Requirement> requirements = new ArrayList<>();
        for (RecordComponent recordComponent : clazz.getRecordComponents()) {
            RequirementFactory requirementFactory = this.factories.get(recordComponent.getType());
            if (requirementFactory == null) {
                throw new Exception("No factory found for type: "
                        + recordComponent.getType().getName());
            }

            Requirement requirement;
            if (recordComponent.getGenericType() instanceof ParameterizedType) {
                requirement = requirementFactory.createRequirement(
                        recordComponent.getName(),
                        recordComponent.getType(),
                        recordComponent.getAnnotations(),
                        (ParameterizedType) recordComponent.getGenericType());
            } else {
                requirement = requirementFactory.createRequirement(
                        recordComponent.getName(), recordComponent.getType(), recordComponent.getAnnotations(), null);
            }
            requirements.add(requirement);
        }

        return requirements;
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

    public List<Requirement> deserializeRequirements(@NotNull JsonNode jsonNode) throws Exception {

        List<Requirement> requirements = new ArrayList<>();
        for (JsonNode node : jsonNode) {
            String name = node.get("name").asText();
            JsonNode data = node.get("data");

            RequirementFactory factory =
                    this.serializers.get(Class.forName(name).asSubclass(Requirement.class));
            if (factory == null) {
                throw new Exception("No deserializer found for requirement type: " + name);
            }

            Requirement requirement = factory.deserialize(objectMapper, data);
            requirements.add(requirement);
        }
        return requirements;
    }

    public RequirementFactory getFactory(@NotNull Requirement requirement) {
        return this.serializers.get(requirement.getClass());
    }

    public Map<Class<?>, RequirementFactory> getFactories() {
        return factories;
    }
}
