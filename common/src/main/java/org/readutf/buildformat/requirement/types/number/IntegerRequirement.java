package org.readutf.buildformat.requirement.types.number;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.types.NumberRequirement;

public class IntegerRequirement extends NumberRequirement<Integer> {

    public IntegerRequirement(
            @JsonProperty("name") @NotNull String name,
            @JsonProperty("min") Integer min,
            @JsonProperty("max") Integer max) {
        super(name, min, max);
    }
}
