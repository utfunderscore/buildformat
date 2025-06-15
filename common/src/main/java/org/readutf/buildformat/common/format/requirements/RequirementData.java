package org.readutf.buildformat.common.format.requirements;

import com.fasterxml.jackson.annotation.JsonTypeInfo;


public record RequirementData(
        RequirementType type,
        int minimumAmount
) {
}
