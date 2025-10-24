package org.readutf.buildformat.requirement.factory.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.factory.RequirementFactory;
import org.readutf.buildformat.requirement.types.SimpleRequirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;

public class SimpleRequirementFactory implements RequirementFactory {

    @Override
    public Requirement createRequirement(@NotNull String name, @NotNull Class<?> type, @NotNull Annotation[] annotations, @Nullable ParameterizedType parameterizedType) {
        return new SimpleRequirement(name, type);
    }

    @Override
    public JsonNode serialize(@NotNull ObjectMapper objectMapper, Requirement requirement) {
        return objectMapper.valueToTree(requirement);
    }

    @Override
    public Requirement deserialize(@NotNull ObjectMapper objectMapper, JsonNode jsonNode) throws JsonProcessingException {
        return objectMapper.treeToValue(jsonNode, SimpleRequirement.class);
    }

    @Override
    public Class<? extends Requirement> getRequirementType() {
        return SimpleRequirement.class;
    }



}
