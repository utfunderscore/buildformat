package org.readutf.buildformat.requirement.types;

import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.types.Cuboid;

public class CuboidRequirement implements Requirement {

    private final String name;

    public CuboidRequirement(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
