package org.readutf.buildformat;

import org.readutf.buildformat.requirement.AttributeRequirement;
import org.readutf.buildformat.requirement.PositionRequirement;

import java.util.ArrayList;
import java.util.List;

public record BuildFormat(
        String name,
        List<PositionRequirement> positionRequirements,
        List<AttributeRequirement> attributeRequirements
) {

    public BuildFormat(String name, List<PositionRequirement> positionRequirements, List<AttributeRequirement> attributeRequirements) {
        this.name = name;
        this.positionRequirements = new ArrayList<>(positionRequirements);
        this.attributeRequirements = new ArrayList<>(attributeRequirements);
    }
}
