package org.readutf.buildformat;

import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.PositionRequirement;
import org.readutf.buildformat.types.Position;

import java.util.ArrayList;
import java.util.List;

public record BuildFormat(
        String name,
        List<Requirement> requirements
) {

    public BuildFormat(String name, List<Requirement> requirements) {
        this.name = name;
        this.requirements = new ArrayList<>(requirements);
    }

    /**
     *
     * @return
     */
    public void validate(BuildMarkerData buildData) {

        for (Requirement requirement : requirements) {
            String attributeValue = buildData.genericSettings().get(requirement.attributeName());
            if (attributeValue == null) {
                throw new IllegalArgumentException("Attribute requirement not met: " + requirement.attributeName());
            }
        }


    }

}
