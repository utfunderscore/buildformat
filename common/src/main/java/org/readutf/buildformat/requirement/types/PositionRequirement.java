package org.readutf.buildformat.requirement.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.readutf.buildformat.requirement.Requirement;

public record PositionRequirement(String name) implements Requirement {

    @JsonCreator
    public PositionRequirement(@JsonProperty("name") String name) {
        this.name = name;
    }
}
