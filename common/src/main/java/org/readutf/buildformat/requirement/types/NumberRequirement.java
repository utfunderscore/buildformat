package org.readutf.buildformat.requirement.types;

import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.Requirement;

import java.util.Objects;

public abstract class NumberRequirement<T extends Number> implements Requirement {
    private final @NotNull String name;
    private final T min;
    private final T max;

    public NumberRequirement(@NotNull String name, T min, T max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }
}
