package org.readutf.buildformat.requirement;

import com.fasterxml.jackson.core.type.TypeReference;
import org.readutf.buildformat.requirement.factory.RequirementFactory;

public record RegisteredRequirement(TypeReference<?> reference, RequirementFactory factory) {}
