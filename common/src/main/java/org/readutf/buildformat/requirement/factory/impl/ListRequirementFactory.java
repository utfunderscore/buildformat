package org.readutf.buildformat.requirement.factory.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.annotations.Max;
import org.readutf.buildformat.requirement.annotations.Min;
import org.readutf.buildformat.requirement.annotations.Range;
import org.readutf.buildformat.requirement.factory.RequirementFactory;
import org.readutf.buildformat.requirement.types.ListRequirement;
import org.readutf.buildformat.requirement.types.StringRequirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;

public class ListRequirementFactory implements RequirementFactory {
    private final BuildFormatManager buildFormatManager;

    public ListRequirementFactory(BuildFormatManager buildFormatManager) {
        this.buildFormatManager = buildFormatManager;
    }

    @Override
    public Requirement createRequirement(
            @NotNull String name,
            @NotNull Class<?> type,
            @NotNull Annotation[] annotations,
            @Nullable ParameterizedType parameterizedType)
            throws Exception {
        if (parameterizedType == null) throw new Exception("ParameterizedType is required for List types");

        Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];

        Requirement innerRequirement =
                switch (actualTypeArgument) {
                    case AnnotatedParameterizedType annotatedType ->
                        createRequirement(
                                name, List.class, annotatedType.getAnnotations(), (ParameterizedType) annotatedType);

                    case ParameterizedType inner -> createRequirement(name, List.class, annotations, inner);
                    case Class<?> innerClass -> {
                        RequirementFactory factory = buildFormatManager.getFactories().get(innerClass);
                        if (factory == null) {
                            throw new Exception("No factory found for type: " + innerClass.getName());
                        }
                        yield factory.createRequirement(name, innerClass, annotations, null);
                    }
                    case WildcardType _ ->
                        throw new Exception("Wildcard types are not supported: " + actualTypeArgument.getTypeName());
                    default -> throw new Exception("Unsupported type argument: " + actualTypeArgument.getTypeName());
                };

        int min = 0;
        int max = Integer.MAX_VALUE;

        for (Annotation annotation : annotations) {
            switch (annotation) {
                case Max maxAnnotation -> max = Math.toIntExact(maxAnnotation.value());
                case Min minAnnotation -> min = Math.toIntExact(minAnnotation.value());
                case Range rangeAnnotation -> {
                    min = rangeAnnotation.min();
                    max = rangeAnnotation.max();
                }
                default -> {}
            }
        }

        return new ListRequirement(name, innerRequirement, max, min);
    }

    @Override
    public JsonNode serialize(ObjectMapper objectMapper, Requirement requirement) throws Exception {
        ListRequirement listRequirement = (ListRequirement) requirement;
        RequirementFactory innerFactory = buildFormatManager.getFactory(listRequirement.innerRequirement());
        return objectMapper.valueToTree(Map.of(
                "name", requirement.getName(),
                "innerRequirement", innerFactory.serialize(objectMapper, listRequirement.innerRequirement()),
                "innerType", innerFactory.getRequirementType().getName(),
                "max", listRequirement.max(),
                "min", listRequirement.min()));
    }

    @Override
    public Requirement deserialize(ObjectMapper objectMapper, JsonNode jsonNode) throws Exception {
        String name = jsonNode.get("name").asText();
        JsonNode innerNode = jsonNode.get("innerRequirement");
        int max = jsonNode.get("max").asInt();
        int min = jsonNode.get("min").asInt();
        String innerType = jsonNode.get("innerType").asText();


        RequirementFactory innerFactory = buildFormatManager.getSerializers().get(Class.forName(innerType).asSubclass(Requirement.class));
        if (innerFactory == null) {
            throw new Exception("No factory found for type: " + innerType);
        }
        Requirement innerRequirement = innerFactory.deserialize(objectMapper, innerNode);
        return new ListRequirement(name, innerRequirement, max, min);
    }

    @Override
    public Class<? extends Requirement> getRequirementType() {
        return ListRequirement.class;
    }
}
