package org.readutf.buildformat.common.format.requirements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface RequirementType {

    @JsonIgnore
    String getRegex();

    String getExplanation();

}
