package org.readutf.buildformat.requirement.types;

import org.readutf.buildformat.requirement.Requirement;

import java.util.List;

public record ListRequirement(String name, Requirement innerRequirement, int max, int min) implements Requirement {

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getType() {
        return List.class;
    }
}
