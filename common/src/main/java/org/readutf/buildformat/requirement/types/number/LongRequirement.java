package org.readutf.buildformat.requirement.types.number;

import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.types.NumberRequirement;

public class LongRequirement extends NumberRequirement<Long> {

    public LongRequirement(@NotNull String name, Long min, Long max) {
        super(name, min, max);
    }
}
