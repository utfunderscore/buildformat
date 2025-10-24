package org.readutf.buildformat.requirement.types;

import org.readutf.buildformat.requirement.Requirement;

public record SimpleRequirement(String name, Class<?> type) implements Requirement {

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getType() {
        return type;
    }
}
