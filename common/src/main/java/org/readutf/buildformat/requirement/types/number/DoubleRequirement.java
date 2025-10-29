package org.readutf.buildformat.requirement.types.number;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.types.NumberRequirement;

public class DoubleRequirement extends NumberRequirement<Double> {

    @JsonCreator
    public DoubleRequirement(
            @JsonProperty("name") @NotNull String name,
            @JsonProperty("min") Double min,
            @JsonProperty("max") Double max) {
        super(name, min, max);
    }
}
