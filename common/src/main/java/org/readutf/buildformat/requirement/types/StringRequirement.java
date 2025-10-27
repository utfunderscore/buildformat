package org.readutf.buildformat.requirement.types;

import org.readutf.buildformat.requirement.Requirement;

public class StringRequirement implements Requirement {

    private final String name;

    public StringRequirement(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
