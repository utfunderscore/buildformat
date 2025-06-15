package org.readutf.buildformat.common.format.requirements.impl;

import org.readutf.buildformat.common.format.requirements.RequirementType;

public record RegexRequirement(
        String regex
) implements RequirementType {
    @Override
    public String getRegex() {
        return regex;
    }
}
