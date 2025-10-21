package org.readutf.buildformat;

import org.readutf.buildformat.requirement.AttributeRequirement;
import org.readutf.buildformat.requirement.PositionRequirement;
import org.readutf.buildformat.types.Position;

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

    /**
     *
     * @return
     */
    public void validate(BuildMarkerData buildData) {

        for (PositionRequirement positionRequirement : positionRequirements) {
            List<Position> positions = buildData.positions().get(positionRequirement.name());
            if (positions == null || positions.size() < positionRequirement.minimumRequired() || positions.size() > positionRequirement.maximumRequired()) {
                throw new IllegalArgumentException("Position requirement not met: " + positionRequirement.name());
            }
        }

        for (AttributeRequirement attributeRequirement : attributeRequirements) {
            String attributeValue = buildData.genericSettings().get(attributeRequirement.attributeName());
            if (attributeValue == null) {
                throw new IllegalArgumentException("Attribute requirement not met: " + attributeRequirement.attributeName());
            }
        }


    }

}
