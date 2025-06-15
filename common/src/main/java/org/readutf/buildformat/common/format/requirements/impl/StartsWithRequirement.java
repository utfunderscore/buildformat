package org.readutf.buildformat.common.format.requirements.impl;

import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.readutf.buildformat.common.format.requirements.RequirementType;

public record StartsWithRequirement(
        String startsWith
) implements RequirementType {
    @Override
    public String getRegex() {
        return "^" + startsWith + ".*";
    }
}
