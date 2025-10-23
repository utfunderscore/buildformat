package org.readutf.buildformat.requirement;

public record Requirement(
        Class<?> dataType,
        String attributeName
) {}
