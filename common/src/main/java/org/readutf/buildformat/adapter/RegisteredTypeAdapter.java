package org.readutf.buildformat.adapter;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Type;

public record RegisteredTypeAdapter(
    Type inputType,
    Type outputType,
    TypeAdapter<?, ?> adapter
) {
}
