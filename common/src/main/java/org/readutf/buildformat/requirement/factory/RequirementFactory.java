package org.readutf.buildformat.requirement.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.requirement.Requirement;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;

public interface RequirementFactory {

    Requirement createRequirement(
            @NotNull String name,
            @NotNull Class<?> type,
            @NotNull Annotation[] annotations,
            @Nullable ParameterizedType parameterizedType)
            throws Exception;

    JsonNode serialize(ObjectMapper objectMapper, Requirement requirement) throws Exception;

    Requirement deserialize(ObjectMapper objectMapper, JsonNode jsonNode) throws Exception;

    Class<? extends Requirement> getRequirementType();
}
