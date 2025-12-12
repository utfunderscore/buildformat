package org.readutf.buildformat.requirement.factory.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.factory.RequirementFactory;
import org.readutf.buildformat.requirement.types.StringRequirement;
import org.readutf.buildformat.requirement.types.number.IntegerRequirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;

public class StringRequirementFactory implements RequirementFactory {
    @Override
    public Requirement createRequirement(
            @NotNull String name,
            @NotNull Class<?> type,
            @NotNull Annotation[] annotations,
            @Nullable ParameterizedType parameterizedType)
            throws Exception {
        return new StringRequirement(name);
    }

    @Override
    public JsonNode serialize(@NotNull ObjectMapper objectMapper, Requirement requirement) throws Exception {
        return objectMapper.valueToTree(requirement);
    }

    @Override
    public Requirement deserialize(@NotNull ObjectMapper objectMapper, JsonNode jsonNode) throws Exception {
        return objectMapper.treeToValue(jsonNode, StringRequirement.class);
    }

    @Override
    public Class<? extends Requirement> getRequirementType() {
        return StringRequirement.class;
    }
}
