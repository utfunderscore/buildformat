package org.readutf.buildformat.requirement.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.readutf.buildformat.requirement.Requirement;

public class PositionRequirement implements Requirement {

    private final String name;

    @JsonCreator
    public PositionRequirement(@JsonProperty("name") String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
