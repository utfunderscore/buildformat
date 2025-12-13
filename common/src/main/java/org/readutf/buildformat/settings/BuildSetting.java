package org.readutf.buildformat.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.utils.DynamicTypeParser;

import java.lang.reflect.Type;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record BuildSetting<T>(
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@typeReference") T data,
        String typeName) {

    public BuildSetting(T data, @NotNull TypeReference<T> typeReference) {
        this(data, typeReference.getType().toString());
    }

    @JsonIgnore
    public Type getType() throws ClassNotFoundException {
        return DynamicTypeParser.parseType(typeName);
    }

}