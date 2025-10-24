package org.readutf.buildformat.requirement.factory.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.annotations.Max;
import org.readutf.buildformat.requirement.annotations.Min;
import org.readutf.buildformat.requirement.annotations.Range;
import org.readutf.buildformat.requirement.factory.RequirementFactory;
import org.readutf.buildformat.requirement.types.NumberRequirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;

public class NumberRequirementFactory<T extends Number> implements RequirementFactory {

    private final T defaultMin;
    private final T defaultMax;

    public NumberRequirementFactory(T defaultMin, T defaultMax) {
        this.defaultMin = defaultMin;
        this.defaultMax = defaultMax;
    }

    @Override
    public Requirement createRequirement(
            @NotNull String name,
            @NotNull Class<?> type,
            @NotNull Annotation[] annotations,
            @Nullable ParameterizedType parameterizedType)
            throws Exception {
        T min = defaultMin;
        T max = defaultMax;

        for (Annotation annotation : annotations) {
            switch (annotation) {
                case Min minAnnotation -> min = (T) (Number) minAnnotation.value();
                case Max maxAnnotation -> max = (T) Number.class.cast(maxAnnotation.value());
                case Range rangeAnnotation -> {
                    min = (T) Number.class.cast(rangeAnnotation.min());
                    max = (T) Number.class.cast(rangeAnnotation.max());
                }
                default -> {}
            }
        }

        return new NumberRequirement<T>(name, (Class<T>) type, defaultMin, defaultMax);
    }

    @Override
    public JsonNode serialize(ObjectMapper objectMapper, Requirement requirement) throws Exception {
        return objectMapper.valueToTree(requirement);
    }

    @Override
    public Requirement deserialize(ObjectMapper objectMapper, JsonNode jsonNode) throws Exception {
        return objectMapper.treeToValue(jsonNode, NumberRequirement.class);
    }

    @Override
    public Class<? extends Requirement> getRequirementType() {
        return NumberRequirement.class;
    }
}
