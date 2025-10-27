package org.readutf.buildformat.requirement.types.number;

import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.types.NumberRequirement;

public class IntegerRequirement extends NumberRequirement<Integer> {

    public IntegerRequirement(@NotNull String name, Integer min, Integer max) {
        super(name, min, max);
    }
}
