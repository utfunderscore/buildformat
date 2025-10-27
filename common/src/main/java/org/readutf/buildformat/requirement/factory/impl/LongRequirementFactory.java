package org.readutf.buildformat.requirement.factory.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.factory.RequirementFactory;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.requirement.types.number.DoubleRequirement;
import org.readutf.buildformat.requirement.types.number.LongRequirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;

public class LongRequirementFactory implements RequirementFactory {

    @Override
    public Requirement createRequirement(
            @NotNull String name,
            @NotNull Class<?> type,
            @NotNull Annotation[] annotations,
            @Nullable ParameterizedType parameterizedType) {
        return new LongRequirement(name, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Override
    public JsonNode serialize(@NotNull ObjectMapper objectMapper, Requirement requirement) {
        return objectMapper.valueToTree(requirement);
    }

    @Override
    public Requirement deserialize(@NotNull ObjectMapper objectMapper, JsonNode jsonNode) throws JsonProcessingException {
        return objectMapper.treeToValue(jsonNode, LongRequirement.class);
    }

    @Override
    public Class<? extends Requirement> getRequirementType() {
        return LongRequirement.class;
    }
}
