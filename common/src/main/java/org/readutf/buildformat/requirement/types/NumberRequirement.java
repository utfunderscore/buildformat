package org.readutf.buildformat.requirement.types;

import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.Requirement;

public record NumberRequirement<T extends Number>(@NotNull String name, @NotNull Class<T> type, T min, T max) implements Requirement {
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getType() {
        return type;
    }
}
