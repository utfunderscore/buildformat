package org.readutf.buildformat.common.format.requirements.impl;

import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.readutf.buildformat.common.format.requirements.RequirementType;

import java.util.regex.Pattern;

public record ExactRequirement(
        String name
) implements RequirementType {

    @Override
    public String getRegex() {
        return "^" + Pattern.quote(name) + "$";
    }
}
